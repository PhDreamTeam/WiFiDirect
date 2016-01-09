package com.example.android.wifidirect;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.android.wifidirect.utils.BatteryInfo;
import com.example.android.wifidirect.utils.LinuxUtils;
import com.example.android.wifidirect.utils.ProcessInfo;
import com.example.android.wifidirect.utils.SystemInfo;

/**
 * Created by AT DR on 08-01-2016
 * .
 */
public class MeteringActivity extends Activity {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metering_activity);

        context = this;

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
                Log.i("MeteringActivity", "Process info -> " + pInfoStr);

                runScreenOffTask(new Runnable() {
                    @Override
                    public void run() {
                        testSystemClockTick();
                    }
                }, 3000);
            }
        });
    }

    BroadcastReceiver screenOffBrdcstReceiver = null;

    /*
     * This method runs Runnable in a new thread, after startTime time.
     * Plays notification sound before and after the Runnable
     */
    public void runScreenOffTask(final Runnable run, final int startTime) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        screenOffBrdcstReceiver = new BroadcastReceiver() {
            // only call runnable once
            boolean firstTime = true;

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("MeteringActivity", "Screen off");
                unregisterReceiver(screenOffBrdcstReceiver);

                if (firstTime && run != null) {
                    firstTime = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(startTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //soundNotification();
                            notification(0, "Test", "Started...");
                            run.run();
                            //soundNotification();
                            notification(0, "Test", "Finished...");
                        }
                    }).start();

                }
            }
        };

        registerReceiver(screenOffBrdcstReceiver, filter);
    }

    /*
     * Create and launch a notification with sound
     */
    public void notification(int notificationID, String title, String text) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification noti = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.icon)
                .setSound(soundUri)
                //.setLargeIcon(R.drawable.icon)
                .build();


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // hide the notification after its selected
        //noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(notificationID, noti);

    }

    /*
     * can be delete, not used anymore
     */
    public void soundNotification() {

        final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
        //tg.release();
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