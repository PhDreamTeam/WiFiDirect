package com.example.android.accesspoint;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.wifidirect.R;

/**
 * Created by DR AT on 05/10/2015.
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

        getStateAndUpdateGuiWifiApState();
        updateApConfiguration();

        btnEnableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toast("Turning WiFi AP ON!!!!!");
                        wifiApControl.setEnabled(getWifiConfiguration(), true);
                        getStateAndUpdateGuiWifiApState();
                        updateGuiApPeriodically(true);
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

        return wifiAPState;
    }

    public void updateApConfiguration() {
        WifiConfiguration apConf = wifiApControl.getConfiguration();
        tvApConfiguration.setText("AP Configuration: " + apConf +
                "\n preSharedKey = " + apConf.preSharedKey);
    }

    private WifiConfiguration getWifiConfiguration() {
        if (radBtnOpenAP.isChecked())
            return wifiApControl.createWifiConfOpen();
        if (radBtnSecureAP.isChecked())
            return wifiApControl.createWifiConfSecure();
        return wifiApControl.createWifiConfFromExistingConf();
    }
}