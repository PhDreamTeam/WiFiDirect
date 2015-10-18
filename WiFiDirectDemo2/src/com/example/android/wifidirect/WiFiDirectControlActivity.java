package com.example.android.wifidirect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by AT e DR on 23-06-2015.
 * .
 */

public class WiFiDirectControlActivity extends Activity {
    private static final String TAG = "WifiDirectControl";

    Context context;

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;

    private static Method deletePersistentGroupMethod;

    private HashMap<String, HashMap<String, String>> discoveredNodesRecords = new HashMap<>();
    private HashMap<String, WifiP2pDevice> discoveredNodesDevices = new HashMap<>();

    // visual controls
    TextView txtP2pOnOff;
    Button btnP2pOn;
    Button btnP2pOff;
    Button btnWiFiDirectConnectAsGO;
    Button btnWiFiDirectConnectAsClient;

    TextView txtDeviceName;
    TextView txtDeviceStatus;
    TextView txtDeviceGroupFormed;
    TextView txtDeviceIsGO;
    TextView txtGOName;
    TextView txtGOAddress;
    TextView txtNetworkName;
    TextView txtNetworkPassphrase;
    TextView txtPeerDiscoveryState;
    TextView txtSelectedPeer;

    MenuItem discoverPeerView;
    MenuItem stopDiscoverPeerView;

    TextView tvConsole;

    ProgressDialog progressDialog;

    boolean alreadyConnecting = false;

    private final IntentFilter intentFilter = new IntentFilter();

    BroadcastReceiver receiver;

    // ExpandableListView TEST
    ExpandableListAdapter<String, String> expListAdapterPeersWithServices;
    ExpandableListView expListViewPeersWithServices;
    ExpandableListAdapter<String, WifiP2pDevice> expListAdapterPeers;
    ExpandableListView expListViewPeers;

    Menu menu;
    private boolean isPeerDiscoveryActivated = false;

    private WifiP2pDnsSdServiceInfo serviceInfo;
    private WifiP2pManager.ActionListener ndsRegisteredListener;
    private boolean isNDSRegisteredAsGO = false;
    private String selectedPeerName;
    private String selectedPeerAddress;
    private Button btnWiFiDirectSearchServices;
    private Button btnWiFiDirectDisconnect;
    private Button btnClearRegGroups;

    static {
        for (Method method : WifiP2pManager.class.getDeclaredMethods()) {
            switch (method.getName()) {
                case "deletePersistentGroup":
                    deletePersistentGroupMethod = method;
                    break;
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifidirect_control_activity);

        context = getApplicationContext();

        tvConsole = ((TextView) findViewById(R.id.textViewConsole));

        boolean wifiDirectSupported = isWifiDirectSupported(context);
        tvConsole.append("WifiDirectSupported: " + wifiDirectSupported); // first message to console

        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(this, getMainLooper(), null);

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);


        //Control state and buttons
        txtP2pOnOff = (TextView) findViewById(R.id.textViewP2pOnOff);

        // P2P ON button
        btnP2pOn = (Button) findViewById(R.id.buttonP2pOn);
        btnP2pOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsole.append("\n\nbtn P2P On pressed...");
                // TODO
            }
        });

        // P2P OFF button
        btnP2pOff = (Button) findViewById(R.id.buttonP2pOff);
        btnP2pOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsole.append("\n\nbtn P2P Off pressed...");
                // TODO
            }
        });

        // Connect as GO button
        btnWiFiDirectConnectAsGO = (Button) findViewById(R.id.buttonWiFiDirectConnectAsGO);
        btnWiFiDirectConnectAsGO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsole.append("\nbtnWiFiDirectConnectAsGO pressed...");
                connectToPeer("GO", selectedPeerAddress, selectedPeerName);
            }
        });

        // Connect as Client button
        btnWiFiDirectConnectAsClient = (Button) findViewById(R.id.buttonWiFiDirectConnectAsClient);
        btnWiFiDirectConnectAsClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsole.append("\nbtnWiFiDirectConnectAsClient pressed...");
                connectToPeer("Client", selectedPeerAddress, selectedPeerName);
            }
        });

        // disconnect from group button
        btnWiFiDirectDisconnect = (Button) findViewById(R.id.WFDButtonDisconnect);
        btnWiFiDirectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsole.append("\n\nbtnWiFiDirectDisconnect pressed...");
                disconnectFromGroup();
            }
        });

        btnWiFiDirectSearchServices = (Button) findViewById(R.id.WFDButtonSearchServices);
        btnWiFiDirectSearchServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsole.append("\n\nbtnWiFiDirectSearchServices pressed...");
                discoverNsdService();
            }
        });


        // info Controls
        txtDeviceName = (TextView) findViewById(R.id.textViewDeviceName);
        txtDeviceStatus = (TextView) findViewById(R.id.textViewDeviceStatus);
        txtDeviceGroupFormed = (TextView) findViewById(R.id.textViewGroupFormed);
        txtDeviceIsGO = (TextView) findViewById(R.id.textViewIsGroupOwner);
        txtGOName = (TextView) findViewById(R.id.textViewGOName);
        txtGOAddress = (TextView) findViewById(R.id.textViewGOAddress);
        txtNetworkName = (TextView) findViewById(R.id.textViewNetworkName);
        txtNetworkPassphrase = (TextView) findViewById(R.id.textViewNetworkPassphrase);
        txtPeerDiscoveryState = (TextView) findViewById(R.id.textViewPeerDiscoveryState);
        txtSelectedPeer = (TextView) findViewById(R.id.textViewSelectedPeer);


        // ExpandableListViews
        expListViewPeersWithServices = (ExpandableListView) findViewById(R.id.expListViewPeersWithServices);
        expListAdapterPeersWithServices = new ExpandableListAdapter<>(this);
        expListViewPeersWithServices.setAdapter(expListAdapterPeersWithServices);

        expListViewPeers = (ExpandableListView) findViewById(R.id.expListViewPeers);
        expListAdapterPeers = new ExpandableListAdapter<>(this);
        expListViewPeers.setAdapter(expListAdapterPeers);

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

        expListViewPeersWithServices.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long id) {


                String elem = (String) discoveredNodesRecords.keySet().toArray()[groupPosition];

                Toast.makeText(WiFiDirectControlActivity.this, "onGroupClick, selected pos: " + groupPosition
                                + " id: " + id + " elem: " + elem + " group: " +
                                expandableListView.getExpandableListAdapter().getGroup(groupPosition),
                        Toast.LENGTH_SHORT).show();

                selectedPeerName = "" + expandableListView.getExpandableListAdapter().getGroup(groupPosition);
                selectedPeerAddress = elem;

                txtSelectedPeer.setText(selectedPeerName);
                return false;
            }
        });

        // button to clear wifi direct registered groups
        btnClearRegGroups = (Button) findViewById(R.id.btnClearRegGroups);
        btnClearRegGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearRegisteredGroups();
            }
        });

        // register this device as a service provider with rule: Client
        registerNsdService(null, "Client"); /*deviceName null generates a random one*/

        // starts listening to other devices
        discoverNsdService();
    }


    private void discoverNsdService() {

        // listener for Bonjour TXT record
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
        /* Callback includes:
         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */
            @SuppressWarnings("unchecked")
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Toast.makeText(WiFiDirectControlActivity.this, "DnsSdTxtRecord available -" + record.toString(),
                        Toast.LENGTH_SHORT).show();
                discoveredNodesRecords.put(device.deviceAddress, (HashMap<String, String>) record);
                discoveredNodesDevices.put(device.deviceAddress, device);
            }
        };

        // listener for Bonjour service response
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice srcDevice) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
//                resourceType.deviceName = discoveredNodesRecords
//                        .containsKey(resourceType.deviceAddress) ? discoveredNodesRecords
//                        .get(resourceType.deviceAddress).get() : resourceType.deviceName;

                // Add to the custom adapter defined specifically for showing
                // wifi devices.
                String discoveryInfo = discoveredNodesRecords.containsKey(srcDevice.deviceAddress) ?
                        discoveredNodesRecords.get(srcDevice.deviceAddress).toString() : "{no discovery info}";

                expListAdapterPeersWithServices.addDataChild(srcDevice.deviceName,
                        srcDevice.toString() + "\n  Status: " +
                                DeviceListFragment.getDeviceStatus(srcDevice.status) +
                                "\n  " + discoveryInfo);

                Toast.makeText(WiFiDirectControlActivity.this, "serviceAvailable: " + instanceName + ", "
                                + registrationType + ", " + srcDevice.deviceName,
                        Toast.LENGTH_SHORT).show();

//                if (!alreadyConnecting) {
//                    if (discoveredNodesRecords.containsKey(resourceType.deviceAddress)) {
//                        String role = discoveredNodesRecords.get(resourceType.deviceAddress).get("role");
//                        if ("GO".equals(role)) {
//                            Toast.makeText(WiFiDirectControlActivity.this, "GO Found: " + resourceType.deviceName,
//                                    Toast.LENGTH_SHORT).show();
//                            tvConsole.append("\nGO Found: " + resourceType.deviceName);
//
//                            // connect to GO
////                            DEBUG DR AT commented to prevent autoconnect
////                            connectToPeer(resourceType);
////                            alreadyConnecting = true;
//                        }
//                    }
//                }
            }
        };

        p2pManager.setDnsSdResponseListeners(channel, servListener, txtListener);


        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        p2pManager.addServiceRequest(channel,
                serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Success!
                        Toast.makeText(WiFiDirectControlActivity.this, "addServiceRequest: succeeded",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        Toast.makeText(WiFiDirectControlActivity.this, "addServiceRequest: failed with code: " + code,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        p2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
                Toast.makeText(WiFiDirectControlActivity.this, "discoverServices: succeeded",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                if (code == WifiP2pManager.P2P_UNSUPPORTED)
                    Toast.makeText(WiFiDirectControlActivity.this,
                            "discoverServices: failed, P2P isn't supported on this device.",
                            Toast.LENGTH_SHORT).show();
                else Toast.makeText(WiFiDirectControlActivity.this, "discoverServices: failed, error: " + code,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
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

        config.groupOwnerIntent = intendedRole.equalsIgnoreCase("GO") ? 14 : 1; // 15 max(GO), 0 min(Cliente)

        // connect to the GO
        p2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Toast.makeText(context, "Connect: successfully", Toast.LENGTH_LONG).show();
                tvConsole.append("\nConnected: success");
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(context, "Connect failed.", Toast.LENGTH_SHORT).show();
                tvConsole.append("\nConnected: failed");
            }
        });
    }

    public void disconnectFromGroup() {
        tvConsole.append("\nDisconnecting...");

        p2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                tvConsole.append("\n Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                tvConsole.append("\n Disconnect succeeded");
            }
        });
    }

    private boolean isWifiDirectSupported(Context ctx) {
        PackageManager pm = (PackageManager) ctx.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null && info.name.equalsIgnoreCase("android.hardware.wifi.direct")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        // TESTE DEBUG DR AT
//        View discoverPeerView2 = findViewById(R.id.atn_direct_discover);
//        View stopDiscoverPeerView2 = findViewById(R.id.atn_direct_stop_discover);

        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                switch (action) {

                    case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                        tvConsole.append("\n\nBDC: WIFI_P2P_STATE_CHANGED");
                        update_P2P_state_changed(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        tvConsole.append("\n\nBDC: WIFI_P2P_PEERS_CHANGED");
                        update_P2P_peers_changed(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                        tvConsole.append("\n\nBDC: WIFI_P2P_CONNECTION_CHANGED");
                        update_P2P_connection_changed(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                        tvConsole.append("\n\nBDC: WIFI_P2P_THIS_DEVICE_CHANGED");
                        update_P2P_this_device_changed(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                        tvConsole.append("\n\nBDC: WIFI_P2P_DISCOVERY_CHANGED_ACTION");
                        update_P2P_Discovery_Peers_changed(intent);
                        break;
                    default:
                        tvConsole.append("\n\nBroadcast receiver: " + action);
                }

            }
        };
        registerReceiver(receiver, intentFilter);
    }

    private void update_P2P_Discovery_Peers_changed(Intent intent) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
        switch (state) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                txtPeerDiscoveryState.setText("Running");
                isPeerDiscoveryActivated = true;
//                discoverPeerView.setVisible(false);
//                stopDiscoverPeerView.setVisible(true);
                invalidateOptionsMenu();
                tvConsole.append("\n  WifiP2pManager.EXTRA_DISCOVERY_STATE: Running");
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                txtPeerDiscoveryState.setText("Stopped");
                isPeerDiscoveryActivated = false;
//                stopDiscoverPeerView.setVisible(false);
//                discoverPeerView.setVisible(true);
                invalidateOptionsMenu();
                tvConsole.append("\n  WifiP2pManager.EXTRA_DISCOVERY_STATE: Stopped");
                break;
            default:
                txtPeerDiscoveryState.setText("unknown");
                tvConsole.append("\n  WifiP2pManager.EXTRA_DISCOVERY_STATE: Unknown");
                //unknwon state
        }
    }


    /**
     *
     */
    private void update_P2P_state_changed(Intent intent) {
        // UI update to indicate wifi p2p status.
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            tvConsole.append("\n  WiFi P2P state enabled");
            txtP2pOnOff.setText("P2P: ON");
            btnP2pOn.setEnabled(false);
            btnP2pOff.setEnabled(true);
        } else {
            tvConsole.append("\n  WiFi P2P state disabled");
            txtP2pOnOff.setText("P2P: OFF");
            btnP2pOn.setEnabled(true);
            btnP2pOff.setEnabled(false);
        }
    }

    private void update_P2P_peers_changed(Intent intent) {
        WifiP2pDeviceList peers = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        if (peers != null) {
            expListAdapterPeers.clear();
            Collection<WifiP2pDevice> devList = peers.getDeviceList();
            for (WifiP2pDevice dev : devList)
                expListAdapterPeers.addDataChild(dev.deviceName, dev);
            tvConsole.append("\n  devList with size: " + devList.size());
        } else {
            // API less than 18
            p2pManager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    expListAdapterPeers.clear();
                    Collection<WifiP2pDevice> devList = peers.getDeviceList();
                    for (WifiP2pDevice dev : devList)
                        expListAdapterPeers.addDataChild(dev.deviceName, dev);
                    tvConsole.append("\n  devList with size: " + devList.size());
                }
            });
        }
    }

    private void update_P2P_connection_changed(Intent intent) {
        //Broadcast intent action indicating that the state of Wi-Fi p2p
        // connectivity has changed. One extra EXTRA_WIFI_P2P_INFO provides
        // the p2p connection info in the form of a WifiP2pInfo object.
        // Another extra EXTRA_NETWORK_INFO provides the network info in
        // the form of a NetworkInfo. A third extra provides the details of the group.

        WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        tvConsole.append("\n  WifiP2pInfo:\n  " + wifiP2pInfo.toString());
        txtDeviceGroupFormed.setText(wifiP2pInfo.groupFormed ? "Group Formed" : "");
        txtDeviceIsGO.setText(wifiP2pInfo.isGroupOwner ? "is GO" : "");
        txtGOAddress.setText(wifiP2pInfo.groupOwnerAddress == null ? "" : "" + wifiP2pInfo.groupOwnerAddress);

        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        tvConsole.append("\n  NetworkInfo:\n  " + networkInfo.toString());
        //networkInfo.

        WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
        tvConsole.append(
                "\n  WifiP2pGroup:\n " + (wifiP2pGroup == null ? "not available on this API" : wifiP2pGroup.toString()));

        if (wifiP2pGroup != null) {
            txtNetworkName.setText(wifiP2pGroup.getNetworkName());
            txtNetworkPassphrase.setText(wifiP2pGroup.getPassphrase());
            txtGOName.setText(wifiP2pGroup.getOwner() != null ? wifiP2pGroup.getOwner().deviceName : "");

            // get network id - is the p2p persistent group iD
//            Parcel parcel = Parcel.obtain();
//            wifiP2pGroup.writeToParcel(parcel, 0);
//            String networkName = parcel.readString();
            tvConsole.append(
                    "\n-- persistent Group ID (networkID): " + getPersistentGroupIdFromWifiP2PGroup(wifiP2pGroup));

        } else {
            // get wifiP2pGroup in another way for API 16
            p2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null) {
                        txtNetworkName.setText(group.getNetworkName());
                        txtNetworkPassphrase.setText(group.getPassphrase());
                        txtGOName.setText(group.getOwner() != null ? group.getOwner().deviceName : "");
                    } else {
                        txtNetworkName.setText("");
                        txtNetworkPassphrase.setText("");
                        txtGOName.setText("");
                    }
                }
            });
        }

        // update service
        registerNsdService(null, wifiP2pInfo.groupFormed ? "GO" : "Client");
    }

    private void update_P2P_this_device_changed(Intent intent) {
        WifiP2pDevice dev = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        tvConsole.append("\n  WifiP2pDevice:\n  " + dev.toString());
        tvConsole.append("\n   Status: " + DeviceListFragment.getDeviceStatus(dev.status));
        txtDeviceName.setText(dev.deviceName);
        txtDeviceStatus.setText(DeviceListFragment.getDeviceStatus(dev.status));
        //txtDeviceIsGO.setText(dev.isGroupOwner()?"is GO":"");

        if (dev.isGroupOwner()) {
            // register as GO if not registered yet
            if (ndsRegisteredListener != null && !isNDSRegisteredAsGO) {
                p2pManager.removeLocalService(channel, serviceInfo, ndsRegisteredListener); // unregister
                ndsRegisteredListener = null;
            }
            if (ndsRegisteredListener == null) // !isNDSRegisteredAsGO
                registerNsdService(null, "GO"); /*deviceName null generates a random one*/
            isNDSRegisteredAsGO = true;
        } else {
            // remove register as GO if already done
            if (ndsRegisteredListener != null && isNDSRegisteredAsGO) {
                p2pManager.removeLocalService(channel, serviceInfo, ndsRegisteredListener);
                ndsRegisteredListener = null;
            }
            if (ndsRegisteredListener == null)
                registerNsdService(null, "Client"); /*deviceName null generates a random one*/
            isNDSRegisteredAsGO = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


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
        throw new RuntimeException("getPersistentGroupIdFromWifiP2PGroup: not found networkId");
    }

    // MENU TEST
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        stopDiscoverPeerView.setVisible(isPeerDiscoveryActivated);
        discoverPeerView.setVisible(!isPeerDiscoveryActivated);

//        discoverPeerView = menu.findItem(R.id.atn_direct_discover);
//        stopDiscoverPeerView = menu.findItem(R.id.atn_direct_stop_discover);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateOptionsMenu(menu);
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_action_items, menu);
        // get search icons
        discoverPeerView = menu.findItem(R.id.atn_direct_discover);
        stopDiscoverPeerView = menu.findItem(R.id.atn_direct_stop_discover);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.atn_direct_enable:
//                if (p2pManager != null && channel != null) {
//
//                    // Since this is the system wireless settings activity, it's
//                    // not going to send us a result. We will be notified by
//                    // WiFiDeviceBroadcastReceiver instead.
//
//                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
//                } else {
//                    Log.e("wifidirectdemo", "channel or p2pManager is null");
//                }
//                return true;

            case R.id.atn_direct_discover:
//                if (progressDialog != null && progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                }
//                progressDialog = ProgressDialog.show(this, "Press back to cancel", "finding peers", true,
//                        true, new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//
//                            }
//                        });

                p2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectControlActivity.this, "Discovery peers Initiated",
                                Toast.LENGTH_SHORT).show();
                        txtPeerDiscoveryState.setText("Started");


                        // test
//                        progressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectControlActivity.this, "Discovery peers Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                        // test
//                        progressDialog.dismiss();
                    }
                });

                return true;
            case R.id.atn_direct_stop_discover:
                p2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectControlActivity.this, "Stopping Peer Discovery",
                                Toast.LENGTH_SHORT).show();
                        txtPeerDiscoveryState.setText("Stopping");
                        // test
//                        progressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectControlActivity.this, "Stopping Peer Discovery Failed: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                        // test
//                        progressDialog.dismiss();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


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

    public void registerNsdService(String devName, String role) {
        //  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<>();
        record.put("listenPort", String.valueOf(30000));
        record.put("role", role);
        record.put("busyLevel", String.valueOf(1));
        record.put("deviceName", devName == null ? role + (int) (Math.random() * 10) : devName);


        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.

        serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("GO", "_backbone1GO1CR._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        p2pManager.addLocalService(channel, serviceInfo, ndsRegisteredListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
//                Toast.makeText(WiFiDirectControlActivity.this, "Service Discovery registered successfully.",
//                        Toast.LENGTH_SHORT).show();
                tvConsole.append("\n**********Service Discovery registered successfully.");
            }

            @Override
            public void onFailure(int errorCode) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//                Toast.makeText(WiFiDirectControlActivity.this, "Service Discovery register FAILED: " + errorCode,
//                        Toast.LENGTH_SHORT).show();
                tvConsole.append("\nService Discovery register FAILED: " + errorCode);
            }
        });
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
}