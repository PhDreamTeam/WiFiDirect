package com.example.android.wifidirect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.util.*;

/**
 * Created by AT e DR on 23-06-2015.
 *
 */

// renomear para P2PControlActivity
public class WiFiDirectControlActivity extends Activity {
    Context context;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private HashMap<String, HashMap<String, String>> discoveredNodes = new HashMap<>();

    // visual controls
    TextView txtP2pOnOff;
    Button btnP2pOn;
    Button btnP2pOff;
    Button wiFiButton;

    TextView txtDeviceName;
    TextView txtDeviceStatus;
    TextView txtDeviceGroupFormed;
    TextView txtDeviceIsGO;
    TextView txtGOName;
    TextView txtGOAddress;
    TextView txtNetworkName;
    TextView txtNetworkPassphrase;
    TextView txtPeerDiscoveryState;

    MenuItem discoverPeerView;
    MenuItem stopDiscoverPeerView;

    TextView tvConsole;

    ProgressDialog progressDialog;

    boolean alreadyConnecting = false;

    private final IntentFilter intentFilter = new IntentFilter();

    BroadcastReceiver receiver;

    // ExpandableListView TEST
    ExpandableListAdapter<String, String> expListAdapterDiscoverdPeers;
    ExpandableListView expListViewDiscoverdPeers;
    ExpandableListAdapter<String, WifiP2pDevice> expListAdapterBroadcastPeers;
    ExpandableListView expListViewBroadcastPeers;

    Menu menu;
    private boolean isPeerDiscoveryActivated = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifidirect_control_activity);

        context = getApplicationContext();

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        tvConsole = ((TextView) findViewById(R.id.textViewConsole));

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);


        //Control state and buttons
        txtP2pOnOff = (TextView) findViewById(R.id.textViewP2pOnOff);
        btnP2pOn = (Button) findViewById(R.id.buttonP2pOn);
        btnP2pOff = (Button) findViewById(R.id.buttonP2pOff);

        wiFiButton = (Button) findViewById(R.id.buttonWiFiConnect);
        wiFiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssid = "DIRECT-Ds-3-XT1068_34c2";
                String key = "qhHQaCLX";
                tvConsole.append("\nConnecting to WiFi: " + ssid + ", " + key);
                connectToWifi(ssid, key);
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

        // ExpandableListViews
        expListViewDiscoverdPeers = (ExpandableListView) findViewById(R.id.expListViewDiscoveredPeers);
        expListAdapterDiscoverdPeers = new ExpandableListAdapter<>(this);
        expListViewDiscoverdPeers.setAdapter(expListAdapterDiscoverdPeers);

        expListViewBroadcastPeers = (ExpandableListView) findViewById(R.id.expListViewBroadcastPeers);
        expListAdapterBroadcastPeers = new ExpandableListAdapter<>(this);
        expListViewBroadcastPeers.setAdapter(expListAdapterBroadcastPeers);

        //allow the selected areas to grow
        final LinearLayout llWFDDiscoveredPeers = (LinearLayout) findViewById(R.id.WFDLinearLayoutDiscoveredPeers);
        final LinearLayout llWFDBroadcastPeers = (LinearLayout) findViewById(R.id.WFDLinearLayoutBroadcastPeers);
        final LinearLayout llWFDConsole = (LinearLayout) findViewById(R.id.WFDLinearLayoutConsole);

        TextView textViewWFDDiscoveredPeers = (TextView) findViewById(R.id.textViewWFDDiscoveredPeers);
        textViewWFDDiscoveredPeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llWFDDiscoveredPeers, llWFDBroadcastPeers, llWFDConsole);
            }
        });

        TextView textViewWFDBroadcastPeers = (TextView) findViewById(R.id.textViewWFDBroadcastPeers);
        textViewWFDBroadcastPeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llWFDBroadcastPeers, llWFDDiscoveredPeers, llWFDConsole);
            }
        });

        TextView textViewWFDConsole = (TextView) findViewById(R.id.textViewWFDConsole);
        textViewWFDConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llWFDConsole, llWFDBroadcastPeers, llWFDDiscoveredPeers);
            }
        });


        discoverNsdService();
    }


    private void discoverNsdService() {
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
                discoveredNodes.put(device.deviceAddress, (HashMap<String, String>) record);
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
//                resourceType.deviceName = discoveredNodes
//                        .containsKey(resourceType.deviceAddress) ? discoveredNodes
//                        .get(resourceType.deviceAddress).get() : resourceType.deviceName;

                // Add to the custom adapter defined specifically for showing
                // wifi devices.
                String discoveryInfo = discoveredNodes.containsKey(resourceType.deviceAddress) ?
                        discoveredNodes.get(resourceType.deviceAddress).toString() : "{no discovery info}";
                expListAdapterDiscoverdPeers.addDataChild(resourceType.deviceName, resourceType.toString() + "\n  " + discoveryInfo);
//                adapterDiscoveredPeers.add(resourceType.toString() + "\n  " + discoveryInfo);

                Toast.makeText(WiFiDirectControlActivity.this, "serviceAvailable: " + instanceName + ", "
                                + registrationType + ", " + resourceType.deviceName,
                        Toast.LENGTH_SHORT).show();

                if (!alreadyConnecting) {
                    if (discoveredNodes.containsKey(resourceType.deviceAddress)) {
                        String role = discoveredNodes.get(resourceType.deviceAddress).get("role");
                        if ("GO".equals(role)) {
                            Toast.makeText(WiFiDirectControlActivity.this, "GO Found: " + resourceType.deviceName,
                                    Toast.LENGTH_SHORT).show();
                            tvConsole.append("\nGO Found: " + resourceType.deviceName);

                            // connect to GO
                            connectToGO(resourceType);
                            alreadyConnecting = true;
                        }
                    }
                }
            }
        };

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);


        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel,
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

        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

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
    void connectToGO(WifiP2pDevice deviceGO) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceGO.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        config.groupOwnerIntent = 0; // 15 max, 0 min

        Toast.makeText(context, "client in auto connect to: " + deviceGO.deviceName,
                Toast.LENGTH_SHORT).show();
        tvConsole.append("\nAuto connecting to: " + deviceGO.deviceName);


//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
//
//
//        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
//                "Connecting to :" + deviceGO.deviceAddress, true, true
////                        new DialogInterface.OnCancelListener() {
////
////                            @Override
////                            public void onCancel(DialogInterface dialog) {
////                                ((DeviceActionListener) getActivity()).cancelDisconnect();
////                            }
////                        }
//        );

        // connect to the GO
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.

                Toast.makeText(context, "Connect: successfully", Toast.LENGTH_LONG).show();
                tvConsole.append("\nConnected: success");

//                // CHECK NEW CODE ========================================================
//                manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
//                    @Override
//                    public void onGroupInfoAvailable(WifiP2pGroup group) {
//                        if (group == null) {
//                            Toast.makeText(context, "Group is Null On connect success",
//                                    Toast.LENGTH_LONG).show();
//                        } else {
//                            String groupInfo = group.getNetworkName() + " " + group.getPassphrase();
//                            Toast.makeText(context, groupInfo,
//                                    Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });
//                // CHECK END
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(context, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
                tvConsole.append("\nConnected: failed");
            }
        });

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
                        tvConsole.append("\nBDC: WIFI_P2P_STATE_CHANGED");
                        update_P2P_state(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        tvConsole.append("\nBDC: WIFI_P2P_PEERS_CHANGED");
                        update_P2P_PeerList(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                        tvConsole.append("\nBDC: WIFI_P2P_CONNECTION_CHANGED");
                        update_P2P_connection(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                        tvConsole.append("\nBDC: WIFI_P2P_THIS_DEVICE_CHANGED");
                        update_P2P_this_device_changed(intent);
                        break;

                    case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                        tvConsole.append("\nBDC: WIFI_P2P_DISCOVERY_CHANGED_ACTION");
                        update_P2P_Discovery_Peers_changed(intent);
                        break;
                    default:
                        tvConsole.append("\nBroadcast receiver: " + action);
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
    private void update_P2P_state(Intent intent) {
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
            // TODO add listener to the buttons and enable/disable P2P...
        }
    }

    private void update_P2P_PeerList(Intent intent) {
        WifiP2pDeviceList peers = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        if (peers != null) {
            expListAdapterBroadcastPeers.clear();
            Collection<WifiP2pDevice> devList = peers.getDeviceList();
            for (WifiP2pDevice dev : devList)
                expListAdapterBroadcastPeers.addDataChild(dev.deviceName, dev);
        } else {
            // API less than 18
            manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    expListAdapterBroadcastPeers.clear();
                    Collection<WifiP2pDevice> devList = peers.getDeviceList();
                    for (WifiP2pDevice dev : devList)
                        expListAdapterBroadcastPeers.addDataChild(dev.deviceName, dev);
                }
            });
        }
    }

    private void update_P2P_connection(Intent intent) {
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
        tvConsole.append("\n  WifiP2pGroup:\n    " + (wifiP2pGroup == null ? "not available on this API" : wifiP2pGroup.toString()));
        if (wifiP2pGroup != null) {
            txtNetworkName.setText(wifiP2pGroup.getNetworkName());
            txtNetworkPassphrase.setText(wifiP2pGroup.getPassphrase());
            txtGOName.setText(wifiP2pGroup.getOwner() != null ? wifiP2pGroup.getOwner().deviceName : "");
            //            wifiP2pGroup.
        }// TODO else try and get wifiP2pGroup in another way for API 16

        // TODO HERE do it
    }

    private void update_P2P_this_device_changed(Intent intent) {
        WifiP2pDevice dev = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        tvConsole.append("\n  WifiP2pDevice:\n  " + dev.toString());
        tvConsole.append("\n   Status: " + DeviceListFragment.getDeviceStatus(dev.status));
        txtDeviceName.setText(dev.deviceName);
        txtDeviceStatus.setText(DeviceListFragment.getDeviceStatus(dev.status));
        //txtDeviceIsGO.setText(dev.isGroupOwner()?"is GO":"");

        // TODO HERE do it

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
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
//                if (manager != null && channel != null) {
//
//                    // Since this is the system wireless settings activity, it's
//                    // not going to send us a result. We will be notified by
//                    // WiFiDeviceBroadcastReceiver instead.
//
//                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
//                } else {
//                    Log.e("wifidirectdemo", "channel or manager is null");
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

                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectControlActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                        txtPeerDiscoveryState.setText("Started");


                        // test
//                        progressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectControlActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                        // test
//                        progressDialog.dismiss();
                    }
                });

                return true;
            case R.id.atn_direct_stop_discover:
                manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
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


    void connectToWifi(String ssid, String key) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);

        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.priority = 10000;

        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

//        wfc.preSharedKey = "\"".concat(password).concat("\"");

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);


        tvConsole.append("\n-------------1----------------");
        printNetworks(wifiManager);
        tvConsole.append("\n--------------1 end---------------");


        //remember id

        int netId = wifiManager.addNetwork(wifiConfig);
        tvConsole.append("Add network returned -> " + netId);

        if (!wifiManager.disconnect()) {
            tvConsole.append("Disconnect failed");
        }

        boolean connected1 = wifiManager.enableNetwork(netId, true);
        tvConsole.append("Enabled networks returned -> " + connected1);

        tvConsole.append("\n-------------2----------------");
        printNetworks(wifiManager);
        tvConsole.append("\n--------------2 end---------------");

        if (!wifiManager.saveConfiguration()) {
            tvConsole.append("Save configuration failed");
        }
        boolean connected2 = wifiManager.reconnect();
        tvConsole.append("Reconnect (network) returned -> " + connected2);
    }

    void printNetworks(WifiManager wifiManager) {
        List<WifiConfiguration> nets = wifiManager.getConfiguredNetworks();
        tvConsole.append("\n Configured networks:");
        for (WifiConfiguration wfconf : nets) {
            if(wfconf.status == WifiConfiguration.Status.DISABLED)
                continue;
            tvConsole.append("\n..." + wfconf.SSID + " priority " + wfconf.priority + "\n     : " + wfconf.status);
            if (wfconf.SSID.equalsIgnoreCase("\"Vodafone-514A4E\"")) {
                wfconf.status = WifiConfiguration.Status.DISABLED;
                int res = wifiManager.updateNetwork(wfconf);
                tvConsole.append("\n   ----->DISABLED result: " + res);
            }

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

}