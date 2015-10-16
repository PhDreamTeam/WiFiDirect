package com.example.android.accesspoint;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.example.android.wifidirect.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by DR AT on 05/10/2015.
 * .
 */
public class WiFiApActivity extends Activity {

    WifiApControl wifiApControl = null;

    Button btnEnableAP, btnDisableAP;
    private TextView tvApState;
    private TextView tvApConfiguration;

    private Button btnRefresh;
    private RadioButton radBtnOpenAP;
    private RadioButton radBtnSecureAP;
    private RadioButton radBtnInternalConfAP;
    private TextView tvNumAPClients;
    private EditText etApSSID;
    private EditText etApPSK;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_ap_activity);

        wifiApControl = WifiApControl.getInstance(getApplicationContext());

        radBtnOpenAP = (RadioButton) findViewById(R.id.radioButtonOpenAP);
        radBtnSecureAP = (RadioButton) findViewById(R.id.radioButtonSecureAP);
        radBtnInternalConfAP = (RadioButton) findViewById(R.id.radioButtonInternalConfAP);

        btnEnableAP = (Button) findViewById(R.id.buttonAPEnable);
        btnDisableAP = (Button) findViewById(R.id.buttonAPDisable);
        btnRefresh = (Button) findViewById(R.id.buttonRefresh);

        tvApState = (TextView) findViewById(R.id.tvWifiAPState);
        tvApConfiguration = (TextView) findViewById(R.id.tvAPConfiguration);
        tvNumAPClients = (TextView) findViewById(R.id.textViewNumAPClients);

        etApSSID = (EditText) findViewById(R.id.editTextAPSSID);
        etApPSK = (EditText) findViewById(R.id.editTextAPPSK);

        getStateAndUpdateGuiWifiApState();
        updateApConfiguration();

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        btnEnableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WifiConfiguration wc = getWifiConfiguration();
                        if (wc != null) {
                            toast("Turning WiFi AP ON!!!!!");
                            wifiApControl.setEnabled(wc, true);
                            getStateAndUpdateGuiWifiApState();
                            updateGuiApPeriodically(true);
                        }
                    }
                });

        btnDisableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toast("Turning WiFi AP OFF!!!!!");
                        wifiApControl.disable();
                        getStateAndUpdateGuiWifiApState();
                        updateGuiApPeriodically(false);
                    }
                });

        btnRefresh.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toast("Refreshing state!!!!!");
                        getStateAndUpdateGuiWifiApState();
                        updateApConfiguration();
                        Log.e("WiFiApActivity", "refresh...");
                        getClientList(true, new FinishScanListener() {
                            @Override
                            public void onFinishScan(final ArrayList<ClientScanResult> result) {
                                tvNumAPClients.post(new Runnable() {
                                    public void run() {
                                        tvNumAPClients.setText("# AP Clients = " + result.size());
                                    }
                                });

                                Log.e("WiFiApActivity", "AP Clients = " + result);
                            }
                        });
                    }
                });
    }

    private void toast(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    /**
     * should terminate when AP reaches final state, We can pass a callback to be called when termination
     */
    Handler handler = new Handler();

    private void updateGuiApPeriodically(final boolean stopWhenAPEnabled) {

        Runnable runnable = new Runnable() {
            int n = 0;
            String finalState = stopWhenAPEnabled ? "WIFI_AP_STATE_ENABLED" : "WIFI_AP_STATE_DISABLED";

            public void run() {
                // actions
                String wifiApState = getStateAndUpdateGuiWifiApState();
                updateApConfiguration();
                // activated it one more time or not
                if (n < 20 && !wifiApState.equals(finalState))
                    handler.postDelayed(this, 100);
            }
        };

        handler.postDelayed(runnable, 100);
    }

    /**
     * Update gui with wifi AP state
     */
    public String getStateAndUpdateGuiWifiApState() {
        // update main string state message
        String wifiAPState = wifiApControl.getStateStr();
        tvApState.setText("AP State: " + wifiAPState);

        switch (wifiAPState) {
            case "WIFI_AP_STATE_ENABLED":
                tvApState.setBackgroundColor(Color.BLUE);
                break;
            case "WIFI_AP_STATE_DISABLED":
                tvApState.setBackgroundColor(Color.RED);
                break;
            default:
                tvApState.setBackgroundColor(Color.DKGRAY);
        }

        // update buttons visibility
        btnEnableAP.setVisibility(wifiAPState.equals("WIFI_AP_STATE_DISABLED") ? View.VISIBLE : View.GONE);
        btnDisableAP.setVisibility(wifiAPState.equals("WIFI_AP_STATE_ENABLED") ? View.VISIBLE : View.GONE);

        etApSSID.setEnabled(!wifiAPState.equals("WIFI_AP_STATE_ENABLED") /*&& radBtnSecureAP.isChecked()*/);
        etApPSK.setEnabled(!wifiAPState.equals("WIFI_AP_STATE_ENABLED") /*&& radBtnSecureAP.isChecked()*/);

        return wifiAPState;
    }


    /**
     * Gets a list of the clients connected to the Hotspot, reachable timeout is 300
     *
     * @param onlyReachables  {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
     * @param finishListener, Interface called when the scan method finishes
     */
    public void getClientList(boolean onlyReachables, FinishScanListener finishListener) {
        getClientList(onlyReachables, 300, finishListener);
    }

    /**
     * Gets a list of the clients connected to the Hotspot
     *
     * @param onlyReachables   {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
     * @param reachableTimeout Reachable Timout in miliseconds
     * @param finishListener,  Interface called when the scan method finishes
     */
    public void getClientList(final boolean onlyReachables, final int reachableTimeout, final FinishScanListener finishListener) {

        Runnable runnable = new Runnable() {
            public void run() {

                BufferedReader br = null;
                final ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

                                if (!onlyReachables || isReachable) {
                                    result.add(
                                            new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(this.getClass().toString(), e.toString());
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e(this.getClass().toString(), e.getMessage());
                    }
                }

                // Get a handler that can be used to post to the main thread
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        finishListener.onFinishScan(result);
                    }
                };
                mainHandler.post(myRunnable);
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void updateApConfiguration() {
        WifiConfiguration apConf = wifiApControl.getConfiguration();
        tvApConfiguration.setText("AP Configuration: " + apConf +
                "\n preSharedKey = " + apConf.preSharedKey);
    }

    /**
     * @return configuration or null if invalid psk
     */
    private WifiConfiguration getWifiConfiguration() {
        // OPEN AP
        if (radBtnOpenAP.isChecked())
            return wifiApControl.createWifiConfOpen();

        // SECURE AP
        if (radBtnSecureAP.isChecked()) {
            String ssid = etApSSID.getText().toString().trim();
            String psk = etApPSK.getText().toString().trim();
            if (psk.length() < 8) {
                toast("PreSharedKey must have 8 chars minimum");
                return null;
            }
            return wifiApControl.createWifiConfSecure(ssid, psk);
        }

        // AP with current configuration
        return wifiApControl.createWifiConfFromExistingConf();
    }

    class ClientScanResult {
        String ipAddress, hwAddress, deviceName;
        boolean isReachable;

        public ClientScanResult(String deviceName, String hwAddress, String ipAddress, boolean isReachable) {
            this.deviceName = deviceName;
            this.hwAddress = hwAddress;
            this.ipAddress = ipAddress;
            this.isReachable = isReachable;
        }

        public String toString() {
            return deviceName + ", " + hwAddress + ", " + ipAddress + ", " + isReachable;
        }


        public String getDeviceName() {
            return deviceName;
        }

        public String getHwAddress() {
            return hwAddress;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public boolean isReachable() {
            return isReachable;
        }
    }

    interface FinishScanListener {
        void onFinishScan(ArrayList<ClientScanResult> result);
    }

}

