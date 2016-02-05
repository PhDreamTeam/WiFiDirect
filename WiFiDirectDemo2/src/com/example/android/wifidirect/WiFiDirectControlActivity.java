package com.example.android.wifidirect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.*;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.example.android.wifidirect.utils.AndroidUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * Created by AT e DR on 23-06-2015.
 * <p/>
 * Note on Discover Peers:
 * When a device is in an active peer discovery the bonjour recognizes it faster .
 * In the bonjour discovery the devices may not appear initially, but after some
 * tries they will appear - we may use a timer to get it right.
 */

public class WiFiDirectControlActivity extends Activity {
    private static final String TAG = "WifiDirectControl";

    private static final String discoveryServiceCurrentInstanceName = "2015-11";
    private static final String discoveryServiceServiceType = "_backboneHyrax._tcp";

    Context context;

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;

    private static Method deletePersistentGroupMethod;
    private static Method startWpsMethod;

    private HashMap<String, RemoteDeviceServiceInfo> discoveredNodesServicesInfo = new HashMap<>();

    // visual controls
    TextView txtP2pOnOff;
    Button btnP2pOn;
    Button btnP2pOff;
    Button btnWiFiDirectConnectAsGO;
    Button btnWiFiDirectConnectAsClient;

    TextView tvP2PDeviceName;
    TextView tvP2PDeviceStatus;
    TextView txtDeviceGroupFormed;
    TextView txtDeviceIsGO;
    TextView tvCGOGOName;
    TextView tvP2PCGOGOAddress;
    TextView tvP2PCGOGroupName;
    TextView tvP2PCGOGroupPassword;
    TextView txtPeerDiscoveryState;
    TextView tvSelectedPeer;

    MenuItem discoverPeerView;
    MenuItem stopDiscoverPeerView;

    TextView tvConsole;

    private final IntentFilter intentFilter = new IntentFilter();

    BroadcastReceiver broadcastReceiver;

    private ListView listViewPeers;
    private ArrayAdapter<WifiP2PDeviceWrapper> listAdapterPeers;

    private ListView listViewPeersWithServices;
    private ArrayAdapter<RemoteDeviceServiceInfo> listAdapterPeersWithServices;

    private boolean isPeerDiscoveryActivated = false;

    private String initialServiceRole = "Available";

    private WifiP2pDnsSdServiceInfo serviceInfo;
    private WifiP2pManager.ActionListener ndsRegisteredListener;
    private boolean isNDSRegisteredAsGO = false;
    private String selectedPeerName;
    private String selectedPeerAddress;
    private Button btnWiFiDirectSearchServices;
    private Button btnP2PCGODisconnect;
    private Button btnClearRegGroups;

    private long operationStartTime = -1;

    static {
        for (Method method : WifiP2pManager.class.getDeclaredMethods()) {
            switch (method.getName()) {
                case "deletePersistentGroup":
                    deletePersistentGroupMethod = method;
                    break;
                case "startWps":
                    startWpsMethod = method;
                    break;
            }
        }
    }

    private Button btnWiFiDirectSearchPeers;
    private Button btnP2PConnect;
    private RadioButton radioButtonConnectAsGO;
    private RadioButton radioButtonConnectAsClient;
    private TextView textViewWifiDirectP2PONOFF;
    private TextView textViewWifiDirectState;
    private Button btnP2PCCGroupName;
    private TextView tvP2PCCGroupName;
    private TextView tvP2PCCGOIPAddress;
    private TextView tvP2PCCMyAddress;
    private Button btnP2PCCDisconnect;
    private TextView tvP2PCCGOName;
    private LinearLayout linearLayoutConnectTo;
    private LinearLayout linearLayoutConnectedAsClient;
    private LinearLayout linearLayoutConnectedAsGO;
    private TextView tvP2PDeviceIPAddress;
    private TextView tvP2PCGONumberOfClients;
    private Button btnP2PCreateGroup;
    private String deviceName;
    private Button btnStartWps;


    boolean hiddenMethodsAreSupported() {
        return deletePersistentGroupMethod != null;
    }

    /*
     * onCreate
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifidirect_control_activity);

        context = getApplicationContext();
        // place code below this line

        tvConsole = ((TextView) findViewById(R.id.textViewConsole));

        boolean wifiDirectSupported = isWifiDirectSupported(context);
        tvConsole.append("WifiDirectSupported: " + wifiDirectSupported); // first message to console

        // P2P manager initialization
        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(this, getMainLooper(), null);

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);

        // getting gui elements and implementing listeners on buttons

        textViewWifiDirectState = (TextView) findViewById(R.id.textViewWifiDirectState);
        tvP2PDeviceName = (TextView) findViewById(R.id.textViewP2PDeviceName);
        tvP2PDeviceIPAddress = (TextView) findViewById(R.id.textViewP2PDeviceIPAddress);
        tvP2PDeviceStatus = (TextView) findViewById(R.id.textViewP2PDeviceStatus);

        textViewWifiDirectP2PONOFF = (TextView) findViewById(R.id.textViewWifiDirectP2PONOFF);

        // Clear wifi direct registered groups
        btnClearRegGroups = (Button) findViewById(R.id.buttonWiFiDirectClearRegGroups);
        btnClearRegGroups.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearRegisteredGroups();
                if (!hiddenMethodsAreSupported()) {
                    AndroidUtils.showDialog(WiFiDirectControlActivity.this, "Hidden methods not supported!!!",
                            "App will close",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // closes this activity
                                    finish();
                                }
                            });
                }
            }
        });

        // Search zone ================================================

        // search Services
        btnWiFiDirectSearchServices = (Button) findViewById(R.id.buttonWifiDirectSearchServices);
        btnWiFiDirectSearchServices.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                discoverServices();
            }
        });

        // search Peers
        btnWiFiDirectSearchPeers = (Button) findViewById(R.id.buttonWifiDirectSearchPeers);
        btnWiFiDirectSearchPeers.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDiscoverPeers();
            }
        });

        // =====================================================================
        // Connect zone
        linearLayoutConnectTo = (LinearLayout) findViewById(R.id.linearLayoutConnectTo);
        radioButtonConnectAsGO = (RadioButton) findViewById(R.id.radioButtonConnectAsGO);
        radioButtonConnectAsClient = (RadioButton) findViewById(R.id.radioButtonConnectAsClient);
        tvSelectedPeer = (TextView) findViewById(R.id.textViewP2PSelectedPeer);

        btnP2PConnect = (Button) findViewById(R.id.buttonP2PConnect);
        btnP2PConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String conType = radioButtonConnectAsGO.isSelected() ? "GO" : "Client";
                connectToPeer(conType, selectedPeerAddress, selectedPeerName);
            }
        });

        btnP2PCreateGroup = (Button) findViewById(R.id.buttonP2PCreateGroup);
        btnP2PCreateGroup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                operationStartTime = System.currentTimeMillis();
                tvConsole.append("\n\nWFD group creation started");
                Log.d(TAG, "WFD group creation started");
                p2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                tvConsole.append("\nWFD group creation started OK");
                                Log.d(TAG, "WFD group creation started OK");
                            }

                            public void onFailure(int reason) {
                                tvConsole.append("\nWFD CreateGroup failed. Reason: " + reason + "\n");
                                Log.d(TAG, "WFD CreateGroup failed. Reason: " + reason);
                            }
                        }
                );
            }
        });


        // =====================================================================
        // Connected as Client
        linearLayoutConnectedAsClient = (LinearLayout) findViewById(R.id.linearLayoutConnectedAsClient);
        tvP2PCCGroupName = (TextView) findViewById(R.id.textViewP2PCCGroupName);
        tvP2PCCGOName = (TextView) findViewById(R.id.textViewP2PCCGOName);
        tvP2PCCGOIPAddress = (TextView) findViewById(R.id.textViewP2PCCGOIPAddress);
        tvP2PCCMyAddress = (TextView) findViewById(R.id.textViewP2PCCMyAddress);

        btnP2PCCDisconnect = (Button) findViewById(R.id.buttonP2PDisconnectAsClient);
        btnP2PCCDisconnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnectFromGroup();
            }
        });

        // =====================================================================
        // Connected as GO
        linearLayoutConnectedAsGO = (LinearLayout) findViewById(R.id.linearLayoutConnectedAsGO);
        tvP2PCGOGroupName = (TextView) findViewById(R.id.textViewP2PCGOGroupName);
        tvP2PCGOGroupPassword = (TextView) findViewById(R.id.textViewP2PCGOPassword);
        tvP2PCGOGOAddress = (TextView) findViewById(R.id.textViewP2PCGOMyAddress);
        tvP2PCGONumberOfClients = (TextView) findViewById(R.id.textViewP2PCGONumberOfClients);

        btnP2PCGODisconnect = (Button) findViewById(R.id.buttonP2PDisconnectAsGO);
        btnP2PCGODisconnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnectFromGroup();
            }
        });


        // =====================================================================
        // WPS
        btnStartWps = (Button) findViewById(R.id.btnStartWps);
        btnStartWps.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Starting WPS");


                WpsInfo wpsi = new WpsInfo();
                // WpsInfo.PBC and WPS activated: the connection is immediate but opens a window in the GO device
                // the user action is ignored - tested on OPO and NEXUS 9
                wpsi.setup = WpsInfo.PBC;
                // wpsi.pin = "0000";

                startWps(wpsi, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Starting WPS: success");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "Starting WPS: failed");
                    }
                });
            }
        });

        // =====================================================================
        // ListView peers with services and p2p peers
        listViewPeersWithServices = (ListView) findViewById(R.id.listViewPeersWithServices);
        listAdapterPeersWithServices = new ArrayAdapter<>(this, R.layout.list_item2,
                R.id.textView_listView, new ArrayList<RemoteDeviceServiceInfo>());
        listViewPeersWithServices.setAdapter(listAdapterPeersWithServices);

        listViewPeers = (ListView) findViewById(R.id.listViewPeers);
        listAdapterPeers = new ArrayAdapter<>(this, R.layout.list_item2,
                R.id.textView_listView, new ArrayList<WifiP2PDeviceWrapper>());
        listViewPeers.setAdapter(listAdapterPeers);

        //allow the selected areas to grow
        final LinearLayout llWFDPeersWithServices = (LinearLayout) findViewById(R.id.WFDLinearLayoutPeersWithServices);
        final LinearLayout llWFDPeers = (LinearLayout) findViewById(R.id.WFDLinearLayoutPeers);
        final LinearLayout llWFDConsole = (LinearLayout) findViewById(R.id.WFDLinearLayoutConsole);

        // textView Peer services listener - to change size of view
        TextView textViewWFDPeersWithServices = (TextView) findViewById(R.id.textViewWFDPeersWithServices);
        textViewWFDPeersWithServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llWFDPeersWithServices, llWFDPeers, llWFDConsole);
            }
        });

        // textView Peers listener - to change size of view
        TextView textViewWFDPeers = (TextView) findViewById(R.id.textViewWFDPeers);
        textViewWFDPeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llWFDPeers, llWFDPeersWithServices, llWFDConsole);
            }
        });

        // Console click listener - to change size of view
        TextView textViewWFDConsole = (TextView) findViewById(R.id.textViewWFDConsole);
        textViewWFDConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llWFDConsole, llWFDPeers, llWFDPeersWithServices);
            }
        });

        listViewPeers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2PDeviceWrapper item = (WifiP2PDeviceWrapper) parent.getItemAtPosition(position);
                AndroidUtils.showDialog(WiFiDirectControlActivity.this, "P2P Peer details", "" + item.getDevice(),
                        null);
            }
        });

        listViewPeersWithServices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final RemoteDeviceServiceInfo item = (RemoteDeviceServiceInfo) parent.getItemAtPosition(position);
                AndroidUtils.showDialog(WiFiDirectControlActivity.this, "P2P Peer with Services details",
                        "" + item.getServiceRecord() + "\n" +
                                item.getWifiP2PDevice(), null);

                selectedPeerName = item.getWifiP2PDevice().deviceName;
                selectedPeerAddress = item.getWifiP2PDevice().deviceAddress;
                tvSelectedPeer.setText(selectedPeerName);
            }
        });
    }

    /*
     *  This method will be called after the reception of the following broadcast messages:
     *  WIFI_P2P_STATE_CHANGED, WIFI_P2P_CONNECTION_CHANGED and WIFI_P2P_THIS_DEVICE_CHANGED.
     */
    private void initP2PFinalActions() {
        // register this device as a service provider with initial role
        registerNsdService(getDeviceName(), initialServiceRole);
        tvConsole.append("\n Device Name:" + getDeviceName());

        // starts listening to other devices
        // discoverServices();
    }

    private String getDeviceName() {
        return deviceName;
    }

    /*
     * singleton for broadcast receiver
     */
    private BroadcastReceiver getBroadcastReceiver() {
        if (broadcastReceiver == null) {

            broadcastReceiver = new BroadcastReceiver() {

                public void onReceive(Context context, Intent intent) {

                    String action = intent.getAction();
                    switch (action) {

                        // indicate whether Wi-Fi p2p is enabled or disabled
                        case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                            tvConsole.append("\n\nBDC: WIFI_P2P_STATE_CHANGED");
                            update_P2P_state_changed(intent);
                            break;

                        // indicate that the state of Wi-Fi p2p connectivity has changed
                        case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                            tvConsole.append("\n\nBDC: WIFI_P2P_CONNECTION_CHANGED");
                            update_P2P_connection_changed(intent);
                            break;

                        // indicate that this device details have changed
                        case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                            tvConsole.append("\n\nBDC: WIFI_P2P_THIS_DEVICE_CHANGED");
                            update_P2P_this_device_changed(intent);
                            break;

                        // indicating that the available peer list has changed
                        case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                            tvConsole.append("\n\nBDC: WIFI_P2P_PEERS_CHANGED");
                            update_P2P_peers_changed(intent);
                            break;

                        // indicate that peer discovery has either started or stopped
                        case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                            tvConsole.append("\n\nBDC: WIFI_P2P_DISCOVERY_CHANGED_ACTION");
                            update_P2P_Discovery_Peers_changed(intent);
                            break;

                        default:
                            tvConsole.append("\n\nBroadcast broadcastReceiver: " + action);
                    }
                }
            };
        }
        return broadcastReceiver;
    }

    /*
     * register discovery Service
     */
    public void registerNsdService(final String devName, final String role) {
        // avoid initial unprepared call - before having the device name
        if (devName == null)
            return;

        // unregister localService to avoid duplicate entries
        if (serviceInfo != null)
            p2pManager.removeLocalService(channel, serviceInfo, null);

        //  Create a string map containing information about the service.
        Map<String, String> record = new HashMap<>();
        record.put("deviceName", devName);
        record.put("role", role);
        record.put("listenPort", Integer.toString(30000));
        record.put("busyLevel", Integer.toString(1));

        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(discoveryServiceCurrentInstanceName,
                discoveryServiceServiceType, record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        p2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                tvConsole.append("\nService registered: " + devName + ", " + role);
            }

            public void onFailure(int errorCode) {
                tvConsole.append(
                        "\nService Discovery register FAILED, error code: " + errorCode + " for: " + devName + ", " + role);
            }
        });
    }

    /*
     * start discovering services
     */
    private void discoverServices() {

        // clear discovered peers list contents
        listAdapterPeersWithServices.clear();

        p2pManager.clearServiceRequests(channel, null);

        //p2pManager.removeServiceRequest();


        // listener for Bonjour TXT record
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            /**
             * Callback includes:
             * @param fullDomain full domain name: e.g "printer._ipp._tcp.local."
             * @param record TXT record dta as a map of key/value pairs.
             * @param device The device running the advertised service.
             */
            @Override
            @SuppressWarnings("unchecked")
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                tvConsole.append(
                        "\n\n Discover services txtRecord available: " + fullDomain + ", " + record.toString() +
                                ", " + device + "\nDEVICE_NAME: " + device.deviceName + ".");

                if (device.deviceName.trim().isEmpty())
                    return;

                // create auxiliary object
                RemoteDeviceServiceInfo rdsi = new RemoteDeviceServiceInfo((HashMap<String, String>) record,
                        device);

                // update list view of bonjour nodes
                if (!discoveredNodesServicesInfo.containsKey(device.deviceAddress)) {
                    RemoteDeviceServiceInfo rdsi2 = discoveredNodesServicesInfo.get(device.deviceAddress);
                    listAdapterPeersWithServices.remove(rdsi2);
                }
                listAdapterPeersWithServices.add(rdsi);

                // update MAP
                discoveredNodesServicesInfo.put(device.deviceAddress, rdsi);
            }
        };

        // listener for Bonjour service response
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice srcDevice) {
                tvConsole.append("\n\n Discover services available: " + instanceName + ", " +
                        registrationType + ", " + srcDevice);
            }
        };

        // set p2p bonjour response listeners
        p2pManager.setDnsSdResponseListeners(channel, servListener, txtListener);

        // create a new  service request
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
//                discoveryServiceCurrentInstanceName, discoveryServiceServiceType);

        // add service request
        p2pManager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        tvConsole.append("\naddServiceRequest: succeeded");
                    }

                    public void onFailure(int code) {
                        // Command failed.  Checking for P2P_UNSUPPORTED, ERROR, or BUSY
                        tvConsole.append("\naddServiceRequest: failed with code: " + code);
                    }
                });

        // launch services discovery
        startBonjourDiscovering();
    }

    /*
     *
     */
    void startBonjourDiscovering() {
        // launch services discovery
        p2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                //btnWiFiDirectSearchServices.setEnabled(false);
                tvConsole.append("\nDiscover Services: succeeded");
            }

            public void onFailure(int code) {
                if (code == WifiP2pManager.P2P_UNSUPPORTED)
                    tvConsole.append("\nDiscover Services: failed, P2P isn't supported on this device.");
                else tvConsole.append("\nDiscover Services: failed, error: " + code);
            }
        });
    }

    /*
     *
     */
    void connectToPeer(String intendedRole, String peerAddressToConnect, String peerNameToConnect) {
        tvConsole.append("\nconnecting to: " + peerAddressToConnect + " (" + peerNameToConnect + ") as "
                + intendedRole);

        if (peerAddressToConnect == null) {
            tvConsole.append("\nNothing to do...");
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peerAddressToConnect;
        config.wps.setup = WpsInfo.PBC;
        // config.wps.pin = "0001";

        config.groupOwnerIntent = intendedRole.equalsIgnoreCase("GO") ? 14 : 1; // 15 max(GO), 0 min(client)

        // connect to the other WFD device
        p2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                tvConsole.append("\nWFD Connect started.");
                Log.d(TAG, "WFD Connect started.");
            }

            public void onFailure(int reason) {
                tvConsole.append("\nWFD Connected failed. Reason: " + reason);
                Log.d(TAG, "WFD Connected failed. Reason: " + reason);
            }
        });
    }

    /*
     *
     */
    void showConnectedActions(boolean asGO) {
        linearLayoutConnectTo.setVisibility(View.GONE);
        linearLayoutConnectedAsClient.setVisibility(asGO ? View.GONE : View.VISIBLE);
        linearLayoutConnectedAsGO.setVisibility(asGO ? View.VISIBLE : View.GONE);
    }

    /*
     *
     */
    void showDisconnectedActions() {
        linearLayoutConnectTo.setVisibility(View.VISIBLE);
        linearLayoutConnectedAsClient.setVisibility(View.GONE);
        linearLayoutConnectedAsGO.setVisibility(View.GONE);
    }

    /*
     *
     */
    public void disconnectFromGroup() {
        tvConsole.append("\n\nDisconnecting...");
        Log.d(TAG, "Disconnecting...");

        operationStartTime = System.currentTimeMillis();

        p2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                tvConsole.append("\n Disconnect OK");
                Log.d(TAG, "Disconnect OK");
            }

            public void onFailure(int reasonCode) {
                tvConsole.append("\n Disconnect failed. Reason :" + reasonCode);
            }
        });
    }

    /*
     *
     */
    private boolean isWifiDirectSupported(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null && info.name.equalsIgnoreCase("android.hardware.wifi.direct")) {
                return true;
            }
        }
        return false;
    }

    /*
     *
     */
    private void startDiscoverPeers() {
        p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                AndroidUtils.toast(context, "Discover peers started");
            }

            public void onFailure(int reasonCode) {
                AndroidUtils.toast(context, "Discover peers failed: " + reasonCode);
            }
        });
    }

    /*
     *
     */
    private void stopDiscoverPeers() {
        p2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                AndroidUtils.toast(context, "Stopping Discover peers");
            }

            public void onFailure(int reasonCode) {
                AndroidUtils.toast(context, "Stopping Peer Discovery Failed: " + reasonCode);
            }
        });
    }

    /*
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        //toast("Registering broadcast receiver");
        registerReceiver(getBroadcastReceiver(), intentFilter);
    }

    /*
     * Broadcast intent action indicating that peer discovery has either started or stopped.
     * One extra {@link #EXTRA_DISCOVERY_STATE} indicates whether discovery has started
     * or stopped.
     */
    private void update_P2P_Discovery_Peers_changed(Intent intent) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
        switch (state) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                btnWiFiDirectSearchPeers.setEnabled(false);
                tvConsole.append("\n  WifiP2pManager.EXTRA_DISCOVERY_STATE: Running");
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                btnWiFiDirectSearchPeers.setEnabled(true);
                tvConsole.append("\n  WifiP2pManager.EXTRA_DISCOVERY_STATE: Stopped");
                break;
            default: // unknown state
                tvConsole.append("\n  WifiP2pManager.EXTRA_DISCOVERY_STATE: Unknown");
        }
    }


    /**
     * Broadcast intent action to indicate whether Wi-Fi p2p is enabled or disabled. An
     * extra #EXTRA_WIFI_STATE provides the state information as int.
     */
    private void update_P2P_state_changed(Intent intent) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        String msg = "P2P: " + (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED ? "ON" : "OFF");
        tvConsole.append("\n " + msg);
        textViewWifiDirectP2PONOFF.setText(msg);
    }

    /*
     * Broadcast intent action indicating that the available peer list has changed. This
     * can be sent as a result of peers being found, lost or updated.
     *
     * <p> An extra #EXTRA_P2P_DEVICE_LIST provides the full list of
     * current peers. The full list of peers can also be obtained any time with
     * #requestPeers.
     */
    private void update_P2P_peers_changed(Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || !update_P2P_peers_changed_API18(intent)) {
            // API less than 18
            p2pManager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    update_P2P_peers_change_base(peers);
                }
            });
        }
    }

    /*
     * Needs API 18 - JELLY_BEAN_MR2
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean update_P2P_peers_changed_API18(Intent intent) {
        WifiP2pDeviceList peers = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        if (peers == null)
            return false;
        update_P2P_peers_change_base(peers);
        return true;
    }

    /*
     *
     */
    private void update_P2P_peers_change_base(WifiP2pDeviceList peers) {
        listAdapterPeers.clear();

        Collection<WifiP2pDevice> devList = peers.getDeviceList();
        for (WifiP2pDevice dev : devList) {
            listAdapterPeers.add(new WifiP2PDeviceWrapper(dev));
        }
        tvConsole.append("\n  devList with size: " + devList.size());
    }


    /**
     *
     */
    public void startWps(WpsInfo wps, WifiP2pManager.ActionListener listener) {
        invokeQuietly(startWpsMethod, p2pManager, channel, wps, listener);
    }


    /*
     *
     */
    private void logEndOfOperation(String logMsg) {
        // get disconnection end time
        long operationTime = System.currentTimeMillis() - operationStartTime;
        tvConsole.append("\n\n" + logMsg + operationTime);
        Log.d(TAG, logMsg + operationTime);
    }


    /*
     *   Broadcast intent action indicating that the state of Wi-Fi P2P connectivity
     * has changed. One extra {@link #EXTRA_WIFI_P2P_INFO} provides the P2P connection info in
     * the form of a {@link WifiP2pInfo} object. Another extra {@link #EXTRA_NETWORK_INFO} provides
     * the network info in the form of a {@link android.net.NetworkInfo}. A third extra
     * EXTRA_WIFI_P2P_GROUP provides information about the group
     */
    private void update_P2P_connection_changed(Intent intent) {
        // EXTRA_WIFI_P2P_INFO provides the p2p connection info in the form of a WifiP2pInfo object.
        final WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        tvConsole.append("\n  WifiP2pInfo:\n  " + wifiP2pInfo.toString());

        if (!wifiP2pInfo.groupFormed) {
            // device is disconnect
            textViewWifiDirectState.setText("WFD state: DISCONNECTED");

            // log disconnection end time
            logEndOfOperation("WFD device disconnected in (ms) ");

            registerNsdService(getDeviceName(), "Available");
            showDisconnectedActions();
            return;
        }

        // log disconnection end time
        logEndOfOperation("WFD group creation: device connected in (ms) ");

        // device is connected
        showConnectedActions(wifiP2pInfo.isGroupOwner);

        // EXTRA_NETWORK_INFO provides the network info in the form of a NetworkInfo.
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        tvConsole.append("\n  NetworkInfo:\n  " + networkInfo.toString());


        //  EXTRA_WIFI_P2P_GROUP provides the details of the group. Only valid for API >= 18
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 ||
                !update_P2P_connection_changed_API18(intent, wifiP2pInfo)) {

            // get wifiP2pGroup in another way for API 16
            p2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    updateGuiWithP2PGroupInfo(group, wifiP2pInfo);
                }
            });
        }

        // we should update the role only if we have the device name
        if (getDeviceName() != null) {
            registerNsdService(getDeviceName(), wifiP2pInfo.isGroupOwner ? "GO" : "Client");
        } else {
            // prepare initial role to be set when device name is available
            initialServiceRole = wifiP2pInfo.isGroupOwner ? "GO" : "Client";
        }
    }

    /*
     * Needs API 18 - JELLY_BEAN_MR2
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean update_P2P_connection_changed_API18(Intent intent, WifiP2pInfo wifiP2pInfo) {
        WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
        if (wifiP2pGroup == null)
            return false;
        updateGuiWithP2PGroupInfo(wifiP2pGroup, wifiP2pInfo);
        return true;
    }

    /*
     *
     */
    void updateGuiWithP2PGroupInfo(WifiP2pGroup group, WifiP2pInfo wifiP2pInfo) {
        if (group != null) {
            tvConsole.append(
                    "\n-- persistent Group ID (networkID): " + getPersistentGroupIdFromWifiP2PGroup(group));
            String networkName = group.getNetworkName();
            textViewWifiDirectState.setText(
                    "WFD state: " + networkName + (wifiP2pInfo.isGroupOwner ? " (GO)" : ""));

            if (wifiP2pInfo.isGroupOwner) {
                // GO
                tvP2PCGOGroupName.setText(networkName);
                tvP2PCGOGroupPassword.setText(group.getPassphrase());
                tvP2PCGOGOAddress.setText(wifiP2pInfo.groupOwnerAddress.toString().substring(1));
                tvP2PDeviceIPAddress.setText(wifiP2pInfo.groupOwnerAddress.toString().substring(1));
                tvP2PCGONumberOfClients.setText(Integer.toString(group.getClientList().size()));
            } else {
                // Client: show group name, GO name, GO IP, my address
                tvP2PCCGroupName.setText(networkName);
                tvP2PCCGOName.setText(group.getOwner().deviceName);
                tvP2PCCGOIPAddress.setText(wifiP2pInfo.groupOwnerAddress.toString().substring(1));
                tvP2PCCMyAddress.setText(getLocalIpAddress(group.getInterface()));
                tvP2PDeviceIPAddress.setText(getLocalIpAddress(group.getInterface()));
            }
        }
    }

    /*
     *
     */
    public String getLocalIpAddress(String interfaceName) {
        try {
            NetworkInterface netInterface = NetworkInterface.getByName(interfaceName);
            for (Enumeration<InetAddress> enumIpAddr = netInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                Log.i(TAG, "***** IP = " + inetAddress);
                if (inetAddress instanceof Inet4Address)
                    return inetAddress.toString().substring(1);

            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    /*
     * Broadcast intent action indicating that this device details have changed.
     */
    private void update_P2P_this_device_changed(Intent intent) {
        WifiP2pDevice dev = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        tvConsole.append("\n  WifiP2pDevice:\n  " + dev.toString());
        tvConsole.append("\n   Status: " + DeviceListFragment.getDeviceStatus(dev.status));
        Log.d(TAG, "Device status: " + DeviceListFragment.getDeviceStatus(dev.status));

        tvP2PDeviceName.setText(dev.deviceName);
        tvP2PDeviceStatus.setText(DeviceListFragment.getDeviceStatus(dev.status));

        if (deviceName == null) {
            deviceName = dev.deviceName;
            initP2PFinalActions();
        }

        if (DeviceListFragment.getDeviceStatus(dev.status).equals("Connected")) {
            logEndOfOperation("WFD this device connected in (ms): ");
        }

        if (DeviceListFragment.getDeviceStatus(dev.status).equals("Available")) {
            logEndOfOperation("WFD this device available in (ms): ");
        }

        // NOTE: dev.isGroupOwner() always show false
    }

    /*
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(getBroadcastReceiver());
    }


    /*
     *
     */
    int getPersistentGroupIdFromWifiP2PGroup(WifiP2pGroup group) {
        Scanner scan = new Scanner(group.toString());
        while (scan.hasNext()) {
            String token = scan.next();
            if (token.equals("networkId:")) {
                int networkId = scan.nextInt();
                scan.close();
                return networkId;
            }
        }
        scan.close();
        Log.e(TAG, "getPersistentGroupIdFromWifiP2PGroup: not found networkId in WifiP2pGroup: " + group);
        return -1;
    }


    /*
     *
     */
    private void toggleLinearLayoutsWeight(LinearLayout ll, LinearLayout ll2, LinearLayout ll3) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ll.getLayoutParams();
        lp.weight = lp.weight == 1 ? (0.3f) : 1;//(lp.weight == (1/3f) ? (1/3f) : 1);
        ll.setLayoutParams(lp);
        // set other layout weights to 1
        LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) ll2.getLayoutParams();
        lp2.weight = 1;
        ll2.setLayoutParams(lp2);
        LinearLayout.LayoutParams lp3 = (LinearLayout.LayoutParams) ll3.getLayoutParams();
        lp3.weight = 1;
        ll3.setLayoutParams(lp3);
    }


    /*
     * invokeQuietly - used to invoke methods with HIDE annotation
     */
    private static Object invokeQuietly(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    /**
     *
     */
    public void clearRegisteredGroup(int netId, WifiP2pManager.ActionListener listener) {
        invokeQuietly(deletePersistentGroupMethod, p2pManager, channel, netId, listener);
    }


    /**
     * Ugly hack, but getting the actual existing groups is an hidden property
     */
    public void clearRegisteredGroups() {
        for (int i = 0; i < 100; ++i)
            clearRegisteredGroup(i, null);
    }


    /*
     * Wrapper class
     */
    class WifiP2PDeviceWrapper {
        WifiP2pDevice device;

        WifiP2PDeviceWrapper(WifiP2pDevice device) {
            this.device = device;
        }

        @Override
        public String toString() {
            return device.deviceName;
        }

        public WifiP2pDevice getDevice() {
            return device;
        }
    }

    /*
     * class to contain an bonjour node info
     */
    class RemoteDeviceServiceInfo {
        private HashMap<String, String> serviceRecord;
        private WifiP2pDevice wifiP2PDevice;

        public RemoteDeviceServiceInfo(HashMap<String, String> serviceRecord, WifiP2pDevice wifiP2PDevice) {
            this.serviceRecord = serviceRecord;
            this.wifiP2PDevice = wifiP2PDevice;
        }

        public HashMap<String, String> getServiceRecord() {
            return serviceRecord;
        }

        public WifiP2pDevice getWifiP2PDevice() {
            return wifiP2PDevice;
        }

        @Override
        public String toString() {
            return wifiP2PDevice.deviceName;
        }
    }
}