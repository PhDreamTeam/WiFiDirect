package com.example.android.accesspoint;

import android.app.Activity;
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
 *
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

        updateGuiWifiApState();
        updateApConfiguration();

        btnEnableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toast("Turning WiFi AP ON!!!!!");
                        wifiApControl.setEnabled(getWifiConfiguration(), true);
                        updateGuiWifiApState();
                        updateGuiApPeriodically(true);
                    }
                });

        btnDisableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toast("Turning WiFi AP OFF!!!!!");
                        wifiApControl.disable();
                        updateGuiWifiApState();
                        updateGuiApPeriodically(false);
                    }
                });

        btnRefresh.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toast("Refreshing state!!!!!");
                        updateGuiWifiApState();
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
                updateGuiWifiApState();
                updateApConfiguration();
                // activated it one more time or not
                if (n < 10 && !wifiApControl.getStateStr().equals(finalState))
                    handler.postDelayed(this, 200);
            }
        };

        handler.postDelayed(runnable, 200);
    }

    /**
     * Update gui with wifi AP state
     */
    public void updateGuiWifiApState() {
        // update main string state message
        tvApState.setText("AP State: " + wifiApControl.getStateStr());

        // update buttons visibility
        boolean wifiApEnabled = wifiApControl.isWifiApEnabled();
        btnEnableAP.setVisibility(wifiApEnabled ? View.GONE : View.VISIBLE);
        btnDisableAP.setVisibility(wifiApEnabled ? View.VISIBLE : View.GONE);
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