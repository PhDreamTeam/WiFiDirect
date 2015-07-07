package com.example.android.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

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

    Context context;

    private WifiManager wiFiManager;

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_control_activity);

        context = getApplicationContext();
        wiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        tvConsole = ((TextView) findViewById(R.id.tvConsole));

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

        wiFiGetConfiguredNetworksButton = (Button) findViewById(R.id.buttonGetConfiguredNetworks);
        wiFiGetConfiguredNetworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getConfiguredNetworks();
            }
        });

        wiFiConnectButton = (Button) findViewById(R.id.buttonConnectToSelectedNetwork);
        wiFiConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //connect
                //todo ...
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
    }

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

    // create a broadcast receives to receive wifi scan notifications
    BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                scanComplete();
            }
        }
    };

    public void scanComplete() {
        expListViewAdapterScannedNetworks.clear();
        scannedResults.clear();
        // get scan results
        List<ScanResult> networkList = wiFiManager.getScanResults();
        tvConsole.append("\nScan Completed, with results:");
        for (ScanResult network : networkList) {
            tvConsole.append("\n   " + network.SSID);
            expListViewAdapterScannedNetworks.addDataChild(network.SSID, network.toString());
            scannedResults.put(network.SSID, network);
        }
        wiFiScanNetworksButton.setEnabled(true);
        tvConsole.append("\n   ");
        stopScan();
    }

    private void stopScan() {
        unregisterReceiver(scanReceiver);
    }

    private void startScan() {
        registerReceiver(scanReceiver, intentFilter);
        wiFiManager.startScan();
    }

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

    void connectToWifi(String ssid, String key) {
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

//        wfc.preSharedKey = "\"".concat(password).concat("\"");

        //remember id
        int netId = wiFiManager.addNetwork(wifiConfig);
        tvConsole.append("Add network returned -> " + netId);

        boolean connected1 = wiFiManager.enableNetwork(netId, true);
        tvConsole.append("Enabled networks returned -> " + connected1);

        if (!wiFiManager.saveConfiguration()) {
            tvConsole.append("Save configuration failed");
        }

        getConfiguredNetworks();

        if (!wiFiManager.disconnect()) {
            tvConsole.append("Disconnect failed");
        }

        boolean connected2 = wiFiManager.reconnect();
        tvConsole.append("Reconnect (network) returned -> " + connected2);
    }

    // TODO: if the selected network is the network with maximum priority it should use its priority (not more than that)
    private int getMaximumPriorityConfiguredNetwork() {
        if (listConfiguredNetworks == null)
            getConfiguredNetworks();

        if (listConfiguredNetworks == null || listConfiguredNetworks.size() == 0)
            return 0;

        return listConfiguredNetworks.get(0).priority;
    }
}