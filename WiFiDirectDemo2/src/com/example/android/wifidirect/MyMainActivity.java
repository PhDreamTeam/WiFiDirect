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
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.accesspoint.WiFiApActivity;
import com.example.android.accesspoint.WifiApControl;
import com.example.android.wifidirect.utils.Logger;
import com.example.android.wifidirect.utils.LoggerSession;

import java.io.File;

/**
 * Created by AT DR on 08-05-2015
 * .
 */
public class MyMainActivity extends Activity {
    public static String APP_MAIN_FILES_DIR_PATH = "/sdcard/Android/data/com.hyrax.atdr";
    public static Logger logger = new Logger("WFD APP");
    public static String TAG = "MainActivity";

    private MyMainActivity myThis;
    private Context context;
    private ConnectivityManager connManager;
    private NetworkInfo wifiNetworkInfo;
    private Button btnWFDGroupOwner;
    private Button btnWFDClient;
    private Button btnWFDControl;
    private Button btnWiFiControl;
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
    private Button btnLogMsg;
    private TextView etLogMsg;
    private TextView etLogDir;

    /**
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        // set logcat also to file
        // Logger.setLogCatToFile(context);

        setContentView(R.layout.my_main_activity);
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

        btnWFDControl = (Button) findViewById(R.id.buttonMainWFDControl);
        btnWiFiControl = (Button) findViewById(R.id.buttonMainWiFiControl);

        btnRelay = (Button) findViewById(R.id.buttonMainRelay);
        btnClient = (Button) findViewById(R.id.buttonMainClient);

        btnMainWiFiAP = (Button) findViewById(R.id.buttonMainWiFiAP);

        btnShowInfo = (Button) findViewById(R.id.buttonShowInfo);

        etLogDir = (TextView) findViewById(R.id.etMALogDir);
        etLogMsg = (TextView) findViewById(R.id.editTextLogMsg);
        btnLogMsg = (Button) findViewById(R.id.btnMALogMsg);

        if (!isWifiOnDevice()) {
            tvMainWiFiState.setText("WiFI not supported");
            btnMainWiFiTurnOn.setVisibility(View.GONE);
            btnMainWiFiTurnOff.setVisibility(View.GONE);
            enableAllWiFiActivityButtons(false, false);
            return;
        }

        if (!p2pSupported) {
            enableAllWiFiActivityButtons(true, false);
            btnWFDControl.setText("P2P not supported");
        }

        adjustWifiApControlButton();

        setButtonsListeners();

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Just tests

       // getDir_withGetExternalFilesDir("logs");

//        try {
//            PrintWriter pw = new PrintWriter(openFileOutput("f2.txt", MODE_WORLD_READABLE));
//            pw.println("olá");
//            pw.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        //getFile2(this, "logs2");
    }



    public File getFile2(Context context, String fileName) {
        // Get the directory for the app's private pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (!file.mkdirs()) {
            Log.e("Logger", "Directory not created");
        }
        return file;
    }

    private void getDir_withGetExternalFilesDir(String dirName) {
        File fileDir = getFile(this, dirName);
        Log.d("New File", "Get dir: " + fileDir.toString());

//        try {
//            PrintWriter pw = new PrintWriter(fileDir);
//            pw.println("Olá");
//            pw.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    public File getFile(Context context, String fileName) {
        // Get the directory for the app's private pictures directory.
        File file = new File(context.getExternalFilesDir(null), fileName);
        if (!file.mkdirs()) {
            Log.e("Logger", "Directory not created:" + file.toString());
        }
        return file;
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
    public String getLogDir() {
        return etLogDir.getText().toString().trim();
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

        btnWFDControl.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "P2P/Wi-Fi Direct Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiDirectControlActivity.class);
                        intent.putExtra("role", "Client");
                        startActivity(intent);
                    }
                });

        btnWiFiControl.setOnClickListener(
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
                        intent.putExtra("logDir", getLogDir());
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
                Intent intent = new Intent(myThis, MeteringActivity.class);
                startActivity(intent);
            }
        });

        btnLogMsg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = etLogMsg.getText().toString();
                LoggerSession ls = logger.getNewLoggerSession(msg);
                ls.close(btnLogMsg.getContext());
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
        btnWFDControl.setEnabled(enable && p2pSuported);
        btnWiFiControl.setEnabled(enable);
        btnRelay.setEnabled(enable);
        btnClient.setEnabled(enable);
    }
}