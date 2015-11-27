package com.example.android.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.accesspoint.WiFiApActivity;
import com.example.android.accesspoint.WifiApControl;
import com.example.android.wifidirect.utils.AndroidUtils;
import com.example.android.wifidirect.utils.LinuxUtils;
import com.example.android.wifidirect.utils.ProcessInfo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by AT DR on 08-05-2015
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
    private boolean p2pSupported = false;
    private WifiManager wifiManager;

    private final IntentFilter intentFilter = new IntentFilter();
    BroadcastReceiver receiver;
    private Button btnMainWiFiAP;
    private Button btnShowInfo;

    /**
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set log also to file
        setLogToFile();

        setContentView(R.layout.my_main_activity);
        context = getApplicationContext();
        myThis = this;

        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        //checking if WiFIDirect is supported
        p2pSupported = isWifiDirectSupported(context);

        tvMainWiFiState = (TextView) findViewById(R.id.textViewMainWiFiState);
        btnMainWiFiTurnOn = (Button) findViewById(R.id.buttonMainWiFiTurnOn);
        btnMainWiFiTurnOff = (Button) findViewById(R.id.buttonMainWiFiTurnOFF);

        btnWFDGroupOwner = (Button) findViewById(R.id.buttonMainWFDGroupOwner);
        btnWFDClient = (Button) findViewById(R.id.buttonMainWFDClient);

        btnP2PWFDClient = (Button) findViewById(R.id.buttonMainP2PWFDClient);
        btnWiFiClient = (Button) findViewById(R.id.buttonMainWiFiClient);

        btnRelay = (Button) findViewById(R.id.buttonMainRelay);
        btnClient = (Button) findViewById(R.id.buttonMainClient);

        btnMainWiFiAP = (Button) findViewById(R.id.buttonMainWiFiAP);

        btnShowInfo = (Button) findViewById(R.id.buttonShowInfo);

        if (!isWifiOnDevice()) {
            tvMainWiFiState.setText("WiFI not supported");
            btnMainWiFiTurnOn.setVisibility(View.GONE);
            btnMainWiFiTurnOff.setVisibility(View.GONE);
            enableAllWiFiActivityButtons(false, false);
            return;
        }

        if (!p2pSupported) {
            enableAllWiFiActivityButtons(true, false);
            btnP2PWFDClient.setText("P2P not supported");
        }

        adjustWifiApControlButton();

        setButtonsListeners();
    }

    /**
     * Add log to file
     */
    private void setLogToFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd_HH'h'mm'm'ss's'");
        String timestamp = sdf.format(new Date());

        String appName = getResources().getString(R.string.app_name);
        appName = appName.replace(' ', '-');
        String cmd = "logcat -v time -f " + "/storage/sdcard0/logs/" + timestamp + "_" + appName + ".txt";

        File logDir = new File("/storage/sdcard0/logs");
        if (!logDir.exists())
            logDir.mkdir();

        try {
            Runtime.getRuntime().exec(cmd);
//            if(btnMainWiFiTurnOff == null)
//                throw new RuntimeException("ehhhhhh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        adjustWifiStateButtons();

        if (receiver == null) {
            receiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    switch (action) {
                        case WifiManager.WIFI_STATE_CHANGED_ACTION:
                            adjustWifiStateButtons();
                            break;
                    }
                }
            };
        }
        registerReceiver(receiver, intentFilter);
    }

    /**
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     *
     */
    private void adjustWifiStateButtons() {
        boolean wifiActive = isWifiActive();
        if (wifiActive) {
            btnMainWiFiTurnOff.setVisibility(View.VISIBLE);
            btnMainWiFiTurnOn.setVisibility(View.GONE);
            tvMainWiFiState.setText("WiFi state: ON");
        } else {
            btnMainWiFiTurnOff.setVisibility(View.GONE);
            btnMainWiFiTurnOn.setVisibility(View.VISIBLE);
            tvMainWiFiState.setText("WiFi state: OFF");
        }
        enableAllWiFiActivityButtons(wifiActive, p2pSupported);
    }

    /**
     *
     */
    private void adjustWifiApControlButton() {
        if (!WifiApControl.isSupported()) {
            btnMainWiFiTurnOff.setVisibility(View.GONE);
        }
    }

    /**
     *
     */
    private void setButtonsListeners() {

        btnMainWiFiTurnOn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Turning WiFi ON!!!!!", Toast.LENGTH_SHORT).show();
                        wifiManager.setWifiEnabled(true);
                    }
                });

        btnMainWiFiTurnOff.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Turning WiFi OFF!!!!!", Toast.LENGTH_SHORT).show();
                        wifiManager.setWifiEnabled(false);
                    }
                });

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


        btnMainWiFiAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(myThis, WiFiApActivity.class);
                        startActivity(intent);
                    }
                });

        btnShowInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProcessInfo pInfo = LinuxUtils.getProcessInfo();
                String pInfoStr = pInfo.getFormattedString();
                AndroidUtils.toast(context, pInfoStr);
                System.out.println("Showing current process information: " + pInfoStr);
            }
        });

    }

    /**
     *
     */
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

    /**
     *
     */
    private boolean isWifiOnDevice() {
        if (connManager == null)
            return false;
        wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo != null;
    }

    /**
     *
     */
    private boolean isWifiActive() {
        return wifiManager.isWifiEnabled();
    }

    /**
     *
     */
    private void enableAllWiFiActivityButtons(boolean enable, boolean p2pSuported) {
        btnWFDGroupOwner.setEnabled(enable && p2pSuported);
        btnWFDClient.setEnabled(enable && p2pSuported);
        btnP2PWFDClient.setEnabled(enable && p2pSuported);
        btnWiFiClient.setEnabled(enable);
        btnRelay.setEnabled(enable);
        btnClient.setEnabled(enable);
    }
}