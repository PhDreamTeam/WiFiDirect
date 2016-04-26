package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import pt.unl.fct.hyrax.wfmobilenetwork.accesspoint.WiFiApActivity;
import pt.unl.fct.hyrax.wfmobilenetwork.accesspoint.WifiApControl;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.Configurations;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.Logger;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Scanner;

/**
 * Created by AT DR on 08-05-2015
 * .
 */
public class MainActivity extends Activity {
    public static String APP_MAIN_FILES_DIR_PATH = "/sdcard/Android/data/pt.unl.fct.hyrax.wfmobilenetwork";
    public static final String CONFIG_FILENAME = "config.txt";

    public static Logger logger = new Logger("WFD APP");
    public static String TAG = "MainActivity";

    private MainActivity myThis;
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
    private Button btnSavePriInterface;
    private RadioButton rbWFD;
    private RadioButton rbWF;

    private Configurations configurations;
    private File logCatFile;

    /**
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        //APP_MAIN_FILES_DIR_PATH = getFilesDir().toString();

        // set logcat also to file
        logCatFile = Logger.setLogCatToFile(context);

        setContentView(R.layout.main_activity);
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

        btnSavePriInterface = (Button) findViewById(R.id.btnMASavePriInterface);
        rbWFD = (RadioButton) findViewById(R.id.rbMAWFD);
        rbWF = (RadioButton) findViewById(R.id.rbMAWF);

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

        // read configurations from file
        configurations = Configurations.readFromConfigurationsFile();

        // show priority interface configuration
        rbWFD.setChecked(configurations.isPriorityInterfaceWFD());
        rbWF.setChecked(configurations.isPriorityInterfaceWF());

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

        // get task file and process it
        String taskFile = getIntent().getStringExtra("taskFile");
        Log.d(TAG, "Task File: " + taskFile);
        if (taskFile != null)
            parseAndExecuteTaskFile(taskFile);
    }

    /**
     *
     */
    public File getFile2(Context context, String fileName) {
        // Get the directory for the app's private pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (!file.mkdirs()) {
            Log.e("Logger", "Directory not created");
        }
        return file;
    }

    /**
     *
     */
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

    /**
     *
     */
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

        // update log file
        AndroidUtils.executeMediaScanFile(logCatFile, context);
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

        btnSavePriInterface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // read GUI state and update configurations
                if (rbWFD.isChecked())
                    configurations.setPriorityInterfaceWFD();
                if (rbWF.isChecked())
                    configurations.setPriorityInterfaceWF();

                // save configurations to file
                configurations.saveToConfigurationsFile();
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
     * Parse and execute task file. File contents will be placed on a String and sent to the activity
     */
    private void parseAndExecuteTaskFile(String taskFile) {
        Scanner scan = null;
        try {
            scan = new Scanner(new File(APP_MAIN_FILES_DIR_PATH + "/" + taskFile));

            StringBuilder sb = new StringBuilder(10 * 1024);

            String activityName = null;

            // read file lines
            while (scan.hasNextLine()) {
                // read line
                String line = scan.nextLine().trim();
                // skip empty and commentary lines
                if (line.isEmpty() || line.charAt(0) == '%')
                    continue;

                //Log.d(TAG, "line: " + line);

                // line should have only one '=' character
                if (line.indexOf('=') == -1 || line.indexOf('=') != line.lastIndexOf('=')) {
                    // TODO review log information
                    throw new IllegalStateException("Task file with malformed line: " + line);
                }

                String token = line.substring(0, line.indexOf('=')).trim();
                String value = line.substring(line.indexOf('=') + 1).trim();

                Log.d(TAG, "line token: " + token + ", value:" + value);

                if (token.equalsIgnoreCase("activity")) {
                    if (activityName != null)
                        throw new IllegalStateException("It is supported only one activity, found : " + activityName +
                                " & " + value);
                    activityName = value.toLowerCase();
                } else
                    sb.append(token).append('=').append(value).append(';');
            }
            // enf of file reading

            // call the activity
            if (activityName != null) {
                Log.d(TAG, "Activity name: " + activityName);

                Intent intent = null;

                if (activityName.equalsIgnoreCase("client"))
                    intent = new Intent(myThis, ClientActivity.class);
                else if (activityName.equalsIgnoreCase("metering"))
                    intent = new Intent(myThis, MeteringActivity.class);
                else throw new IllegalStateException("Activity not supported (yet): " + activityName);

                intent.putExtra("taskStr", sb.toString());
                startActivity(intent);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scan != null)
                scan.close();
        }
    }

    /**
     * Put all parameters in Map and return it
     */
    public static HashMap<String, String> getParamsMap(String taskStr) {
        // the map
        HashMap<String, String> map = new HashMap<>();
        // slip the string
        String[] params = taskStr.split(";");
        // process params
        for (String param : params) {
            // split by '='
            String[] parts = param.split("=");
            // check for repetitions
            if (map.containsKey(parts[0]))
                throw new IllegalStateException("Task string with repeated param: " + param);
            // keep param token and value
            map.put(parts[0], parts[1]);
        }
        // return map
        return map;
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
    private void enableAllWiFiActivityButtons(boolean enable, boolean p2pSupported) {
        btnWFDGroupOwner.setEnabled(enable && p2pSupported);
        btnWFDClient.setEnabled(enable && p2pSupported);
        btnWFDControl.setEnabled(enable && p2pSupported);
        btnWiFiControl.setEnabled(enable);
        btnRelay.setEnabled(enable);
        btnClient.setEnabled(enable);
    }
}

