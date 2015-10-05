package com.example.android.accesspoint;

import android.app.Activity;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_ap_activity);

        wifiApControl = WifiApControl.getInstance(getApplicationContext());

        btnEnableAP = (Button) findViewById(R.id.buttonAPEnable);
        btnDisableAP = (Button) findViewById(R.id.buttonAPDisable);
        btnRefresh = (Button) findViewById(R.id.buttonRefresh);


        tvApState = (TextView) findViewById(R.id.tvWifiAPState);
        tvApConfiguration = (TextView) findViewById(R.id.tvAPConfiguration);

        updateApState();
        updateApConfiguration();

        boolean apEnabled = wifiApControl.isEnabled();
        btnEnableAP.setVisibility(apEnabled ? View.GONE : View.VISIBLE);
        btnDisableAP.setVisibility(apEnabled ? View.VISIBLE : View.GONE);

        btnEnableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(), "Turning WiFi AP ON!!!!!", Toast.LENGTH_SHORT).show();
                        wifiApControl.setEnabled(wifiApControl.getConfiguration(), true);
                        btnEnableAP.setVisibility(View.GONE);
                        btnDisableAP.setVisibility(View.VISIBLE);
                        updateApState();
                    }
                });
        btnDisableAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(), "Turning WiFi AP OFF!!!!!", Toast.LENGTH_SHORT).show();
                        wifiApControl.setEnabled(wifiApControl.getConfiguration(), false);
                        btnDisableAP.setVisibility(View.GONE);
                        btnEnableAP.setVisibility(View.VISIBLE);
                        updateApState();
                    }
                });

        btnRefresh.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(), "Refreshing state!!!!!", Toast.LENGTH_SHORT).show();
                        updateApState();
                        updateApConfiguration();
                        Log.e("WiFiApActivity", "refresh...");
                    }
                });
    }

    public void updateApState() {
        tvApState.setText("AP State: " + wifiApControl.getStateStr());
    }

    public void updateApConfiguration() {
        WifiConfiguration apConf = wifiApControl.getConfiguration();
        tvApConfiguration.setText("AP Configuration: " + apConf +
            "\n preSharedKey = " + apConf.preSharedKey);
    }
}