package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by AT DR on 08-05-2015.
 * .
 */

public class MyMainActivity extends Activity {
    MyMainActivity myThis;
    Context context;
    private ConnectivityManager connManager;
    private NetworkInfo wifiNetworkInfo;
    private Button btnWFDGroupOwner;
    private Button btnWFDClient;
    private Button btnP2PWFDClient;
    private Button btnWiFiClient;
    private Button btnRelay;
    private Button btnClient;
    private TextView tvMainWiFiState;
    private Button btnMainWiFiTurnOn;
    private Button btnMainWiFiTurnOff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_main_activity);
        context = getApplicationContext();
        myThis = this;

        // connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // WiFi State

        tvMainWiFiState = (TextView) findViewById(R.id.textViewMainWiFiState);
        btnMainWiFiTurnOn = (Button) findViewById(R.id.buttonMainWiFiTurnOn);
        btnMainWiFiTurnOff = (Button) findViewById(R.id.buttonMainWiFiTurnOFF);

        btnWFDGroupOwner = (Button) findViewById(R.id.buttonMainWFDGroupOwner);
        btnWFDClient = (Button) findViewById(R.id.buttonMainWFDClient);

        btnP2PWFDClient = (Button) findViewById(R.id.buttonMainP2PWFDClient);
        btnWiFiClient = (Button) findViewById(R.id.buttonMainWiFiClient);

        btnRelay = (Button) findViewById(R.id.buttonMainRelay);
        btnClient = (Button) findViewById(R.id.buttonMainClient);

//        if(!isWifiOnDevice()) {
//            tvMainWiFiState.setText("WiFI not supported");
//            btnMainWiFiTurnOn.setVisibility(View.GONE);
//            btnMainWiFiTurnOff.setVisibility(View.GONE);
//            btnWFDGroupOwner.setEnabled(false);
//            btnWFDClient.setEnabled(false);
//            btnP2PWFDClient.setEnabled(false);
//            btnWiFiClient.setEnabled(false);
//            btnRelay.setEnabled(false);
//            btnClient.setEnabled(false);
//            return;
//        }

//        setButtonsListeners();

        // TODO: falta testar isto num telemóvel e verificar se funciona
//        if (!isWifiDirectSupported(context)) {
//            btnP2PWFDClient.setEnabled(false);
//            btnP2PWFDClient.setText("P2P not supported");
//        }
    }

    @Override
    public void onResume() {
        //adjustWifiStateButtons();
    }

    private void adjustWifiStateButtons() {
        if (isWifiActive()) {
            btnMainWiFiTurnOn.setVisibility(View.VISIBLE);
            btnMainWiFiTurnOff.setVisibility(View.GONE);
        } else {
            btnMainWiFiTurnOn.setVisibility(View.GONE);
            btnMainWiFiTurnOff.setVisibility(View.VISIBLE);
        }
    }

    private void setButtonsListeners() {
        btnWFDGroupOwner.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Group Owner!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "GO");
                        startActivity(intent);
                    }
                });

        btnWFDClient.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Wi-Fi Direct Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "Client");
                        startActivity(intent);
                    }
                });

        btnP2PWFDClient.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "P2P/Wi-Fi Direct Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiDirectControlActivity.class);
                        intent.putExtra("role", "Client");
                        startActivity(intent);
                    }
                });

        btnWiFiClient.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "WiFi Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiControlActivity.class);
                        startActivity(intent);
                    }
                });


        btnRelay.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Relay!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, RelayActivity.class);
                        //intent.putExtra("role", "Relay");
                        // teste DR
//                        String CRPort = ((EditText) findViewById(R.id.editTextCRPortNumber)).getText().toString();
//                        Toast toast2 = Toast.makeText(context, CRPort, Toast.LENGTH_SHORT);
//                        toast2.show();
                        //   intent.putExtra("CrPortNumber", ((EditText) findViewById(R.id.editTextCRPortNumber)).getText().toString());

                        startActivity(intent);
                    }
                });

        btnClient.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Simple Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, ClientActivity.class);
//                        intent.putExtra("role", "Client");
//                        intent.putExtra("CrPortNumber", ((EditText) findViewById(R.id.editTextCRPortNumber2)).getText().toString());
//                        intent.putExtra("CrIpAddress", ((EditText) findViewById(R.id.editTextCrIpAddress)).getText().toString());
                        //TODO...
                        startActivity(intent);
                    }
                });
    }

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

    private boolean isWifiOnDevice() {
        if (connManager == null)
            return false;
        wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo != null;
    }

    private boolean isWifiActive() {
        return wifiNetworkInfo.isAvailable();
    }
}