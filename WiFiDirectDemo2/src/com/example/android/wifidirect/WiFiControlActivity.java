package com.example.android.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.example.android.wifidirect.utils.AndroidUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by DR AT on 06/07/2015.
 * <p/>
 * Note: The broadcast executes continuously, in delay of (???) 15 secs, we stopped it
 * when received the first scan
 */
public class WiFiControlActivity extends Activity {

    private static final String TAG = "WifiControl";

    Context context;

    private WifiManager wiFiManager;

    private static Method connectWithWifiConfigurationMethod;

    HashMap<String, ScanResult> scannedResults = new HashMap<>();

    private TextView tvConsole;
    private Button wiFiScanNetworksButton;
    private Button wiFiGetConfiguredNetworksButton;

    ExpandableListView expListViewScannedNetworks;
    ExpandableListAdapter<String, String> expListViewAdapterScannedNetworks;
    IntentFilter intentFilter;
    ExpandableListView expListViewConfiguredNetworks;
    ExpandableListAdapter<String, String> expListViewAdapterConfiguredNetworks;
    TextView tvSelectedNetwork;
    EditText etSelectedNetworkPassword;
    Button wiFiConnectButton;
    private List<WifiConfiguration> listConfiguredNetworks;
    private TextView tvWFLinkSpeedValue;
    private Button btnWifiDisconnect;
    private Button btnWifiGetStatus;
    private Button btnWifiConnectDirectly;
    private TextView tvWFMainState;

    static {
        for (Method method : WifiManager.class.getDeclaredMethods()) {
            switch (method.getName()) {
                case "connect":
                    Class<?>[] parTypes = method.getParameterTypes();
                    if (parTypes.length == 2 && parTypes[0].getSimpleName().equals("WifiConfiguration")) {
                        connectWithWifiConfigurationMethod = method;
                    }
                    break;
            }
        }
    }

    private ScrollView scrollViewConsole;
    private WifiManager.WifiLock wlock;
    private TextView tvDeviceName;
    private TextView tvDeviceIPAddress;
    private TextView tvDeviceStatus;
    private BroadcastReceiver wifiBroadcastReceiver;

    /*
         *
         */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_control_activity);

        context = getApplicationContext();
        wiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        tvConsole = ((TextView) findViewById(R.id.tvConsole));

        // Blue info zone ===========================================
        tvWFMainState = (TextView) findViewById(R.id.textViewWifiState);
        tvDeviceIPAddress = (TextView) findViewById(R.id.textViewWFCADeviceIP);
        tvDeviceStatus = (TextView) findViewById(R.id.textViewWFCAStatus);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        expListViewScannedNetworks = (ExpandableListView) findViewById(R.id.expListViewScannedNetworks);
        expListViewAdapterScannedNetworks = new ExpandableListAdapter<>(this);
        expListViewScannedNetworks.setAdapter(expListViewAdapterScannedNetworks);

        tvSelectedNetwork = ((TextView) findViewById(R.id.textViewSelectedNetwork));
        etSelectedNetworkPassword = ((EditText) findViewById(R.id.editTextSelectedNetworkPassword));

        expListViewScannedNetworks.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int position, long l) {
                String selectedNetwork = (String) expListViewScannedNetworks.getItemAtPosition(position);
                tvSelectedNetwork.setText(selectedNetwork);
                return false;
            }
        });

        expListViewConfiguredNetworks = (ExpandableListView) findViewById(R.id.expListViewConfiguredNetworks);
        expListViewAdapterConfiguredNetworks = new ExpandableListAdapter<>(this);
        expListViewConfiguredNetworks.setAdapter(expListViewAdapterConfiguredNetworks);


        wiFiScanNetworksButton = (Button) findViewById(R.id.btnScanNetworks);
        wiFiScanNetworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wiFiScanNetworksButton.setEnabled(false);
                startScan();
            }
        });

        wiFiGetConfiguredNetworksButton = (Button) findViewById(R.id.btnGetConfiguredNetworks);
        wiFiGetConfiguredNetworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getConfiguredNetworks();
            }
        });

        wiFiConnectButton = (Button) findViewById(R.id.btnWifiConnectToSelectedNetwork);
        wiFiConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //connect
                Toast.makeText(WiFiControlActivity.this, "Connecting... ", Toast.LENGTH_SHORT).show();

                connectToWifi(tvSelectedNetwork.getText().toString(), etSelectedNetworkPassword.getText().toString());
            }
        });

        final LinearLayout llScanNetworks = (LinearLayout) findViewById(R.id.linearLayoutScanNetworks);
        final LinearLayout llConfNetworks = (LinearLayout) findViewById(R.id.linearLayoutConfiguredNetworks);
        final LinearLayout llConsole = (LinearLayout) findViewById(R.id.linearLinearConsole);

        TextView textViewScannedNetworks = (TextView) findViewById(R.id.textViewScannedNetworks);
        textViewScannedNetworks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llScanNetworks, llConfNetworks, llConsole);
            }
        });

        TextView textViewConfigureNetworks = (TextView) findViewById(R.id.textViewConfiguredNetworks);
        textViewConfigureNetworks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llConfNetworks, llScanNetworks, llConsole);
            }
        });

        TextView textViewConsole = (TextView) findViewById(R.id.textViewConsole);
        textViewConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutsWeight(llConsole, llScanNetworks, llConfNetworks);
            }
        });

        tvWFLinkSpeedValue = (TextView) findViewById(R.id.textViewWFLinkSpeedValue);


        btnWifiDisconnect = (Button) findViewById(R.id.btnWifiDisconnect);
        btnWifiGetStatus = (Button) findViewById(R.id.btnWifiGetStatus);
        btnWifiConnectDirectly = (Button) findViewById(R.id.btnWifiConnectDirectly);

        scrollViewConsole = (ScrollView) findViewById(R.id.scrollViewConsole);

        btnWifiConnectDirectly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Not working
                connectDirectlyToWifi(tvSelectedNetwork.getText().toString(),
                        etSelectedNetworkPassword.getText().toString());
            }
        });

        btnWifiDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiDisconnect();
            }
        });

        btnWifiGetStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // boolean wifiConn = isConnectedWifi();
                boolean wifiConn2 = isConnectedWifi2();
                // tvConsole.append("\nWifi Status by wiFiManager.getConnectionInfo: " + wifiConn);
                consoleAppend("Wifi Status by connectivityManager TYPE_WIFI: " + wifiConn2);
                WifiInfo wi = wiFiManager.getConnectionInfo();
                consoleAppend("ConnectionInfo: " + wi);
                tvWFLinkSpeedValue.setText(Integer.toString(wi.getLinkSpeed()));
            }
        });


        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // broadcast broadcastReceiver to receive wifi state changed events
        wifiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    NetworkInfo.State wifiState = info.getState();
                    String msg = "WF con: ";
                    if (wifiState == NetworkInfo.State.CONNECTING || wifiState == NetworkInfo.State.CONNECTED) {
                        msg += wiFiManager.getConnectionInfo().getSSID();
                    } else {
                        msg += wifiState;
                    }
                    tvWFMainState.setText(msg);

                    // device IP
                    if (wifiState == NetworkInfo.State.CONNECTED) {
                        WifiInfo conInfo = wiFiManager.getConnectionInfo();
                        tvDeviceIPAddress.setText("" + AndroidUtils.intToIp(conInfo.getIpAddress()));
                    } else tvDeviceIPAddress.setText("");

                    // connection state
                    tvDeviceStatus.setText(wifiState.toString());
                }
            }
        };

        // register wifi state changed broadcast broadcastReceiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiBroadcastReceiver, intentFilter);

        // rescale the priority of wifi configure networks
        adjustConfiguredWifiNetworkPriorities();

    }

    /*
     *
     */
    private void toggleLinearLayoutsWeight(LinearLayout ll, LinearLayout ll2, LinearLayout ll3) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ll.getLayoutParams();
        lp.weight = lp.weight == 1 ? (0.6f) : 1;//(lp.weight == (1/3f) ? (1/3f) : 1);
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
     *
     */
    // create a broadcast receives to receive wifi scan notifications
    BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                scanComplete();
            }
        }
    };

    /*
     * Reset the priority of wifi configured networks to start at V (70)
     */
    void adjustConfiguredWifiNetworkPriorities() {
        // TODO
    }

    /*
     *
     */
    void consoleAppend(String msg) {
        tvConsole.append("\n   " + msg);

        // TODO do this - not done
        scrollViewConsole.scrollTo(0, tvConsole.getLineCount() * tvConsole.getLineHeight());
        scrollViewConsole.computeScroll();
        scrollViewConsole.fullScroll(View.FOCUS_DOWN);
    }

    /*
         *
         */
    public void scanComplete() {
        expListViewAdapterScannedNetworks.clear();
        scannedResults.clear();
        // get scan results
        List<ScanResult> networkList = wiFiManager.getScanResults();
        consoleAppend("Scan Completed, with results:");
        for (ScanResult network : networkList) {
            consoleAppend("  " + network.SSID);
            expListViewAdapterScannedNetworks.addDataChild(network.SSID, network.toString());
            scannedResults.put(network.SSID, network);
        }
        wiFiScanNetworksButton.setEnabled(true);
        consoleAppend("   ");
        stopScan();
    }

    /*
     *
     */
    private void stopScan() {
        unregisterReceiver(scanReceiver);
    }

    /*
     *
     */
    private void startScan() {
        registerReceiver(scanReceiver, intentFilter);
        wiFiManager.startScan();
    }

    /*
     *
     */
    static class CmpByPriority implements Comparator<WifiConfiguration> {

        static Comparator<WifiConfiguration> CMP = new CmpByPriority();

        static Comparator<WifiConfiguration> getCmp() {
            return CMP;
        }

        @Override
        public int compare(WifiConfiguration wfc0, WifiConfiguration wfc1) {
            // this is a reversed order comparator
            return -1 * (wfc0.priority - wfc1.priority);
        }
    }

    /*
     *
     */
    void getConfiguredNetworks() {
        expListViewAdapterConfiguredNetworks.clear();
        listConfiguredNetworks = wiFiManager.getConfiguredNetworks();

        // sort networks by priority
        Collections.sort(listConfiguredNetworks, CmpByPriority.getCmp());

        for (WifiConfiguration wfconf : listConfiguredNetworks) {
            if (wfconf.status == WifiConfiguration.Status.DISABLED)
                continue;
            expListViewAdapterConfiguredNetworks.addDataChild(
                    wfconf.SSID + " priority: " + wfconf.priority + " status: " + wfconf.status,
                    wfconf.toString());

        }
    }

    /*
     *
     */
    WifiConfiguration buildWifiConfiguration(String ssid, String key) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);

        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.priority = getMaximumPriorityConfiguredNetwork() + 1;

        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        return wifiConfig;
    }

    /*
     *
     */
    void connectToWifi(String ssid, String key) {
        WifiConfiguration wifiConfig = buildWifiConfiguration(ssid, key);

//        wfc.preSharedKey = "\"".concat(password).concat("\"");

        //remember id
        int netId = wiFiManager.addNetwork(wifiConfig);
        consoleAppend("Add network returned -> " + netId);

        boolean connected1 = wiFiManager.enableNetwork(netId, true);
        consoleAppend("Enabled networks returned -> " + connected1);

        if (!wiFiManager.saveConfiguration()) {
            consoleAppend("Save configuration failed");
        }

        getConfiguredNetworks();

        if (!wiFiManager.disconnect()) {
            consoleAppend("Disconnect failed");
        }

        boolean connected2 = wiFiManager.reconnect();
        consoleAppend("Reconnect (network) returned -> " + connected2);

        if (connected2) {
            WifiInfo wi = wiFiManager.getConnectionInfo();
            tvWFLinkSpeedValue.setText(Integer.toString(wi.getLinkSpeed()));
            consoleAppend("WifiInfo = " + wi.toString());
        }
    }

    /*
     *
     */
    void wifiDisconnect() {
        wiFiManager.disconnect();
    }

    /*
     * Not working
     */
    void connectDirectlyToWifi(String ssid, String key) {
        WifiConfiguration wifiConfig = buildWifiConfiguration(ssid, key);
        wifiConnectTo(wifiConfig);
        consoleAppend("Connect directly executed");
    }

    /*
     *
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
     * Not working in Motorolas G
     * If an error occurred invoking the method via reflection, false is returned.
     */
    public void wifiConnectTo(WifiConfiguration wifiConfig) {
        invokeQuietly(connectWithWifiConfigurationMethod, wiFiManager, wifiConfig, null);
    }

    /*
     *
     */
    boolean isConnectedWifi() {
        WifiInfo ci = wiFiManager.getConnectionInfo();
        return ci != null;
    }

    /*
      * This method is more accurate to describe if wifi is connected or not
     */
    public boolean isConnectedWifi2() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }

    /*
     *
     */
    // TODO: if the selected network is the network with maximum priority it should use its priority (not more than that)
    private int getMaximumPriorityConfiguredNetwork() {
        if (listConfiguredNetworks == null)
            getConfiguredNetworks();

        if (listConfiguredNetworks == null || listConfiguredNetworks.size() == 0)
            return 0;

        return listConfiguredNetworks.get(0).priority;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiBroadcastReceiver);
    }
}