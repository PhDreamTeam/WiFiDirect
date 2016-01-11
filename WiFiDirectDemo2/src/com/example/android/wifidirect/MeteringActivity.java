package com.example.android.wifidirect;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.android.wifidirect.utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by AT DR on 08-01-2016
 * .
 */
public class MeteringActivity extends Activity {
    public static String TAG = "MeteringActivity";

    private TextView tvLevelScale;
    private TextView tvVoltage;
    private TextView tvTemperature;
    private Button btnUpdateBatteryInfo;
    private Context context;
    private Button btnGetCPUInfo;
    private TextView tvPID;
    private TextView tvPPID;
    private TextView tvUTime;
    private TextView tvSTime;
    private PowerManager.WakeLock wakeLock;
    private Button btnRunBackgroundTask;
    private NotificationManager notificationManager;
    private Uri soundUri;
    private TextView tvTurnScreenOFF;
    private BroadcastReceiver screenOffBroadcastReceiver = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metering_activity);

        context = this;

        // used with Notifications ============================================
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Battery info ===============================================
        tvLevelScale = (TextView) findViewById(R.id.textViewLevelScale);
        tvVoltage = (TextView) findViewById(R.id.textViewVoltage);
        tvTemperature = (TextView) findViewById(R.id.textViewTemperature);

        btnUpdateBatteryInfo = (Button) findViewById(R.id.btnUpdateBatteryInfo);

        // process cpu info ===============================================
        tvPID = (TextView) findViewById(R.id.textViewMAPID);
        tvPPID = (TextView) findViewById(R.id.textViewMAPPID);
        tvUTime = (TextView) findViewById(R.id.textViewMAUTime);
        tvSTime = (TextView) findViewById(R.id.textViewMASTime);

        btnGetCPUInfo = (Button) findViewById(R.id.btnGetCPUInfo);

        // run background task ===============================================
        btnRunBackgroundTask = (Button) findViewById(R.id.btnMARunBackgroundTask);
        tvTurnScreenOFF = (TextView) findViewById(R.id.textViewMATurnScreenOFF);

        // listeners ===============================================
        btnUpdateBatteryInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BatteryInfo bi = SystemInfo.getBatteryInfo(context);
                tvLevelScale.setText("" + bi.batteryLevel + " / " + bi.batteryScale);
                tvVoltage.setText("" + bi.batteryVoltage / 1000.0 + "V");
                tvTemperature.setText("" + bi.batteryTemperature / 10.0 + "ºC");
                Log.i("MeteringActivity", "Battery info -> " + bi);
            }
        });

        btnGetCPUInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProcessInfo pInfo = LinuxUtils.getProcessInfo();
                tvPID.setText("" + pInfo.pid);
                tvPPID.setText("" + pInfo.ppid);
                tvUTime.setText("" + pInfo.utime * 100);
                tvSTime.setText("" + pInfo.stime * 100);
                String pInfoStr = pInfo.getFormattedString();
                Log.i(TAG, "Process info -> " + pInfoStr);
            }
        });

        btnRunBackgroundTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // create it
                Runnable backgroundTask = new Runnable() {
                    @Override
                    public void run() {
                        testSystemClockTick();
                    }
                };

                // run it
                runScreenOffTask(backgroundTask, 3000);
            }
        });
    }


    /*
     * This method runs Runnable in a new thread, after startTime time.
     * Plays notification sound before and after the Runnable
     */
    public void runScreenOffTask(final Runnable backgroundTask, final int startTime) {

        // avoid nested calls
        btnRunBackgroundTask.setEnabled(false);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        // acquire a partial wake lock, to stay with cpu running when screen off
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WFDMeterActivity");
        wakeLock.acquire();

        screenOffBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(final Context context, Intent intent) {
                Log.i(TAG, "Screen off");
                unregisterReceiver(screenOffBroadcastReceiver);

                // hide turn screen off notification textView
                tvTurnScreenOFF.setVisibility(View.GONE);

                // create a new thread to run delayed and background task
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        // pause for the initial time, to stabilize system
                        try {
                            Thread.sleep(startTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // start notification
                        doNotificationStart();

                        // meter battery consumption
                        final BatteryHelper bh = new BatteryHelper();
                        bh.startReadingBattery(context);

                        // execute background task
                        backgroundTask.run();

                        // end of battery metering
                        bh.stopReadingBattery();

                        // end Notification
                        doNotificationEnd();

                        // activate button
                        btnRunBackgroundTask.post(new Runnable() {
                            @Override
                            public void run() {
                                String msg = "Running time (ms) = " + bh.getRunningTimeMs() +
                                        ", consumed power (mcWh) = " + bh.getConsumedPowerMicroWh();

                                // show dialog to screen
                                AndroidUtils.showDialog(context, "Final values", msg, null);

                                // save values to file
                                writeValuesToFile(msg, "batteryData.txt");

                                // activate button to enable next text
                                btnRunBackgroundTask.setEnabled(true);
                            }
                        });

                        // release wake lock
                        wakeLock.release();
                    }
                }).start();
            }
        };

        // register broadcast receiver for screen off
        registerReceiver(screenOffBroadcastReceiver, filter);

        // warn user to turn screen off
        tvTurnScreenOFF.setVisibility(View.VISIBLE);
    }

    /*
     *
     */
    public void doNotificationStart() {
        notification(0, "Test", "Started...", Color.RED, 100, 1000);
    }

    private void writeValuesToFile(String msg, String filename) {
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd_HH'h'mm'm'ss's'");
        String timestamp = sdf.format(new Date());

        final File f = new File(Environment.getExternalStorageDirectory() + "/"
                + context.getPackageName() + "/" + timestamp + "_" + filename); // add filename
        File dirs = new File(f.getParent());
        if (!dirs.exists())
            dirs.mkdirs();
        //f.createNewFile();
        Log.d(TAG, "Saving final battery values to file: " + f.toString());

        try {
            PrintWriter fos = new PrintWriter(f);
            fos.println(msg);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /*
     *
     */
    public void doNotificationEnd() {
        // led slow flashing
        // notification(0, "Test", "Finished...", Notification.FLAG_SHOW_LIGHTS, 1000, 500);

        // led on
        notification(0, "Test", "Finished...", Color.RED, 1, 0);
    }


    /*
     * Create and launch a notification with sound
     */
    public void notification(int notificationID, String title, String text, int lightArg, int ledOnMs, int ledOffMs) {

        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.icon)
                .setSound(soundUri)
                .setLights(lightArg, ledOnMs, ledOffMs)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        notificationManager.notify(notificationID, notification);
    }


    /*
     *
     */
    private void testSystemClockTick() {
        final int nTests = 10;

        for (int i = 0; i < nTests; ++i) {
            Log.i("MeteringActivity", "Process info " + (i + 1) + " -> " +
                    LinuxUtils.getProcessInfo().getFormattedString());
            if (i < nTests - 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("MeteringActivity", "onDestroy called");
    }

    public void onStart() {
        super.onStart();
        Log.d("MeteringActivity", "onStart called");
    }

    public void onStop() {
        super.onStop();
        Log.d("MeteringActivity", "onStop called");
    }

    public void onPause() {
        super.onPause();
        Log.d("MeteringActivity", "onPause called");
    }

    public void onRestart() {
        super.onRestart();
        Log.d("MeteringActivity", "onRestart called");
    }

    public void onResume() {
        super.onResume();
        Log.d("MeteringActivity", "onResume called");
    }
}

// o que estava a fazer - como detectar que o ecran se desligou (para iniciar os testes)