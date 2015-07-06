package com.example.android.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

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


    private TextView tvConsole;
    private Button wiFiScanNetworksButton;

    ExpandableListView expListViewScannedNetworks;
    ExpandableListAdapter<String, String> expListViewAdapterScannedNetworks;
    IntentFilter intentFilter;
    ExpandableListView expListViewConfiguredNetworks;
    ExpandableListAdapter<String, String> expListViewAdapterConfiguredNetworks;

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

        expListViewConfiguredNetworks = (ExpandableListView) findViewById(R.id.expListViewConfiguredNetworks);
        expListViewAdapterConfiguredNetworks = new ExpandableListAdapter<>(this);
        expListViewConfiguredNetworks.setAdapter(expListViewAdapterConfiguredNetworks);


        wiFiScanNetworksButton = (Button) findViewById(R.id.btnScanNetworks);
        wiFiScanNetworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wiFiScanNetworksButton.setEnabled(false);
                startScan();
//                LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayoutScanNetworks);
//                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ll.getLayoutParams();
//                lp.weight = lp.weight == 1 ? 0.5f : (lp.weight == 0.5f ? 2 : 1);
//                ll.setLayoutParams(lp);
            }
        });

        final LinearLayout llScanNetworks = (LinearLayout) findViewById(R.id.linearLayoutScanNetworks);
        final LinearLayout llConfNetworks = (LinearLayout) findViewById(R.id.linearLayoutConfiguredNetworks);
        final LinearLayout llConsole = (LinearLayout) findViewById(R.id.linearLinearConsole);

        TextView textViewScannedNetworks = (TextView) findViewById(R.id.textViewScannedNetworks);
        textViewScannedNetworks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutWeight(llScanNetworks);
            }
        });

        TextView textViewConfigureNetworks = (TextView) findViewById(R.id.textViewConfiguredNetworks);
        textViewConfigureNetworks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutWeight(llConfNetworks);
            }
        });

        TextView textViewConsole = (TextView) findViewById(R.id.textViewConsole);
        textViewConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLinearLayoutWeight(llConsole);
            }
        });
    }

    private void toggleLinearLayoutWeight(LinearLayout ll) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ll.getLayoutParams();
        lp.weight = lp.weight == 1 ? 2 : (lp.weight == 2 ? 3 : 1);
        ll.setLayoutParams(lp);
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
        // get scan results
        List<ScanResult> networkList = wiFiManager.getScanResults();
        tvConsole.append("\nScan Completed, with results:");
        for (ScanResult network : networkList) {
            tvConsole.append("\n   " + network.SSID);
            expListViewAdapterScannedNetworks.addDataChild(network.SSID, network.toString());
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
        ((WifiManager) getSystemService(Context.WIFI_SERVICE)).startScan();
    }


}