package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

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
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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
    private Button btnRunMulticastReceiver;
    private Button btnRunMulticastSend;
    private EditText etMulticastNetworkInterface;
    private Button btnBatteryAutoUpdateOn;
    private Button btnBatteryAutoUpdateOff;
    private Timer timer;
    private BatteryManager batManager;
    private TextView tvCurrent;
    private Intent intent;
    private Button btnRunCpuTask;
    private Button btnRunFileTask;

    private double dummyDouble;
    private Button btnStopFileTask;

    private Button btnStopCpuTask;
    private boolean runTaskThreadFlag;


    private boolean fileTaskThreadFlag;
    private int counter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metering_activity);

        context = this;

        batManager = new BatteryManager();

        // used with Notifications ============================================
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Battery info ===============================================
        tvLevelScale = (TextView) findViewById(R.id.textViewLevelScale);
        tvVoltage = (TextView) findViewById(R.id.textViewVoltage);
        tvTemperature = (TextView) findViewById(R.id.textViewTemperature);
        tvCurrent = (TextView) findViewById(R.id.textViewMACurrent);

        btnUpdateBatteryInfo = (Button) findViewById(R.id.btnUpdateBatteryInfo);
        btnBatteryAutoUpdateOn = (Button) findViewById(R.id.btnMABatteryAutoUpdateOn);
        btnBatteryAutoUpdateOff = (Button) findViewById(R.id.btnMABatteryAutoUpdateOff);

        // process cpu info ===============================================
        tvPID = (TextView) findViewById(R.id.textViewMAPID);
        tvPPID = (TextView) findViewById(R.id.textViewMAPPID);
        tvUTime = (TextView) findViewById(R.id.textViewMAUTime);
        tvSTime = (TextView) findViewById(R.id.textViewMASTime);

        btnRunCpuTask = (Button) findViewById(R.id.btnMARunCpuTask);
        btnRunFileTask = (Button) findViewById(R.id.btnMARunFileTask);
        btnStopCpuTask = (Button) findViewById(R.id.btnMAStopCpuTask);
        btnStopFileTask = (Button) findViewById(R.id.btnMAStopFileTask);

        btnGetCPUInfo = (Button) findViewById(R.id.btnGetCPUInfo);

        // run background task ===============================================
        btnRunBackgroundTask = (Button) findViewById(R.id.btnMARunBackgroundTask);
        tvTurnScreenOFF = (TextView) findViewById(R.id.textViewMATurnScreenOFF);

        // Multicast background tasks ===============================================
        btnRunMulticastReceiver = (Button) findViewById(R.id.btnMAMulticastReceive);
        btnRunMulticastSend = (Button) findViewById(R.id.btnMAMulticastSend);

        etMulticastNetworkInterface = (EditText) findViewById(R.id.etMAMulticastInterface);

        // ==========================================================================
        intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null)
            throw new IllegalStateException("Battery helper can't register ACTION_BATTERY_CHANGED intent");

        // listeners ===============================================
        btnUpdateBatteryInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BatteryInfo bi = SystemInfo.getBatteryInfo(context);
                if (bi != null) {
                    tvLevelScale.setText("" + bi.batteryLevel + " / " + bi.batteryScale);
                    tvVoltage.setText("" + bi.batteryVoltage / 1000.0 + "V");
                    tvTemperature.setText("" + bi.batteryTemperature / 10.0 + "ºC");
                    Log.i("MeteringActivity", "Battery info -> " + bi);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    BatteryHelper.readBatteryApiLollipop(context);
                    int batteryCurrentNow = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    tvCurrent.setText("" + batteryCurrentNow + "mcA");
                }
            }
        });

        btnBatteryAutoUpdateOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // off actions
                btnBatteryAutoUpdateOn.setVisibility(View.GONE);
                timer.cancel();
                timer = null;
                btnBatteryAutoUpdateOff.setVisibility(View.VISIBLE);
            }
        });

        btnBatteryAutoUpdateOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // on actions
                btnBatteryAutoUpdateOff.setVisibility(View.GONE);
                counter = 0;

                timer = new Timer("batteryTimer");
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        BatteryHelper.readBatteryApiLollipop(context);
                        final BatteryInfo bi = SystemInfo.getBatteryInfo(context);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            final int batteryCurrentNow = batManager.getIntProperty(
                                    BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

                            tvCurrent.post(new Runnable() {
                                @Override
                                public void run() {

                                    if (bi != null) {
                                        tvLevelScale.setText("" + bi.batteryLevel + " / " + bi.batteryScale);
                                        tvVoltage.setText("" + bi.batteryVoltage / 1000.0 + "V");
                                        tvTemperature.setText("" + bi.batteryTemperature / 10.0 + "ºC");
                                        //Log.i("MeteringActivity", "Battery info -> " + bi);
                                    }

                                    tvCurrent.setText("" + (++counter)+ " "+batteryCurrentNow);
                                }
                            });
                        }
                    }
                }, 0, 1000);

                btnBatteryAutoUpdateOn.setVisibility(View.VISIBLE);
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

        btnRunCpuTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRunCpuTask.setVisibility(View.GONE);
                runCpuTask();
                btnStopCpuTask.setVisibility(View.VISIBLE);
            }
        });

        btnStopCpuTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStopCpuTask.setVisibility(View.GONE);
                runTaskThreadFlag = false;
                btnRunCpuTask.setVisibility(View.VISIBLE);
            }
        });

        btnRunFileTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRunFileTask.setVisibility(View.GONE);
                runFileTask();
                btnStopFileTask.setVisibility(View.VISIBLE);
            }
        });

        btnStopFileTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStopFileTask.setVisibility(View.GONE);
                fileTaskThreadFlag = false;
                btnRunFileTask.setVisibility(View.VISIBLE);
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

        btnRunMulticastReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRunMulticastReceiver.setEnabled(false);
                MulticastSocketReceiver msr = new MulticastSocketReceiver(
                        etMulticastNetworkInterface.getText().toString());
                msr.start();
            }
        });

        btnRunMulticastSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MulticastSocketTransmitter mst = new MulticastSocketTransmitter(
                        etMulticastNetworkInterface.getText().toString());
                mst.start();
            }
        });
    }

    /*
     *
     */
    private void runCpuTask() {
        Thread cpuTaskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runTaskThreadFlag = true;
                double y = 1.000000000000000000122;
                for (; runTaskThreadFlag; ) {
                    dummyDouble *= (y *= 1.000000000001) + 0.00000000001;
                }
            }
        });
        cpuTaskThread.start();
    }

    /*
     *
     */
    private void runFileTask() {
        Thread fileTaskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                fileTaskThreadFlag = true;
                String LOG_DIR_BASE_PATH = MainActivity.APP_MAIN_FILES_DIR_PATH;
                AndroidUtils.buildPath(LOG_DIR_BASE_PATH);

                for (; fileTaskThreadFlag; ) {
                    try {
                        final File f = new File(LOG_DIR_BASE_PATH + "/WD_testFileTask.txt");
                        PrintWriter pw = new PrintWriter(f);
                        pw.print("Test message");
                        pw.close();
                        f.delete();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        fileTaskThread.start();
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

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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


    /**
     * MulticastSocket are limited in the range of 224.0.0.1 to 239.255.255.255.
     * Multicast ports between: 1025 and 49151
     * <p/>
     * Incorporate Multicast in client context
     */
    public class MulticastSocketTransmitter extends Thread {
        final static String INET_ADDR = "224.0.0.5";
        final static int PORT = 8888;

        String netInterface = null;

        /*
         *
         */
        public MulticastSocketTransmitter(String netInterface) {
            this.netInterface = netInterface.trim();
        }

        /*
         *
         */
        public void run() {

            byte buffer[] = new byte[512];

            try {

                WifiManager wim = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wim != null) {
                    WifiManager.MulticastLock mcLock = wim.createMulticastLock(TAG);
                    mcLock.acquire();
                } else {
                    throw new RuntimeException("Failed to get context.WIFI_SERVICE");
                }


                // Get the address that we are going to connect to.
                InetAddress addr = InetAddress.getByName(INET_ADDR);

                // get multicast socket, in a dynamic port
                MulticastSocket txSocket = new MulticastSocket();
                txSocket.setTimeToLive(20);

                // the socket interface with a specified interface
                NetworkInterface netInterface = AndroidUtils.getNetworkInterface(this.netInterface);
                Log.d(TAG, "Multicast transmitter: will use network interface: " + netInterface);
                if (netInterface != null)
                    txSocket.setNetworkInterface(netInterface);

                for (int i = 0; i < 5; i++) {
                    String msg = "This is message no " + i;

                    // buffer: first 4 bytes contains the string length
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putInt(msg.length());
                    System.arraycopy(buf.array(), 0, buffer, 0, 4);

                    // put msg on buffer
                    System.arraycopy(msg.getBytes(), 0, buffer, 4, msg.getBytes().length);

                    // Create a packet that will contain the data (in the form of bytes) and send it
                    // to the well know multicast address and port
                    DatagramPacket msgPacket = new DatagramPacket(buffer, buffer.length, addr, PORT);
                    txSocket.send(msgPacket);

                    Log.d(TAG, "Packet sent with msg: " + msg);
                    Thread.sleep(500);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * TODO final: ver a velocidade de envio
     * <p/>
     * It is necessary to pass the network interface used to communicate. Like: wlan0, p2p-p2p0-1, ...
     */
    public class MulticastSocketReceiver extends Thread {
        final static String INET_ADDR = MulticastSocketTransmitter.INET_ADDR;
        final static int PORT = MulticastSocketTransmitter.PORT;

        String netInterface = null;

        /*
         *
         */
        public MulticastSocketReceiver(String netInterface) {
            this.netInterface = netInterface.trim();
        }

        /*
         *
         */
        public void run() {

            // Create a buffer of bytes, which will be used to store
            // the incoming bytes containing the information from the server.
            // Since the message is small here, 256 bytes should be enough.
            byte[] buf = new byte[256];

            try {

                WifiManager wim = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wim != null) {
                    WifiManager.MulticastLock mcLock = wim.createMulticastLock(TAG);
                    mcLock.acquire();
                } else {
                    throw new RuntimeException("Failed to get context.WIFI_SERVICE");
                }

                // get multicast socket in a local port - must be the same
                MulticastSocket multicastReceiverSocket = new MulticastSocket(PORT);
                // the multicast socket  in a well know port
                InetSocketAddress iSock = new InetSocketAddress(INET_ADDR, PORT);

                // join the socket to the well know address, with a specified interface
                NetworkInterface netInterface = AndroidUtils.getNetworkInterface(this.netInterface);
                Log.d(TAG, "Multicast receiver: will use network interface: " + netInterface);
                multicastReceiverSocket.joinGroup(iSock, netInterface);

                Log.d(TAG, "Multicast receiver: receiving at " + INET_ADDR + ":" + PORT);

                while (true) {
                    // Receive the information and print it.
                    DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                    multicastReceiverSocket.receive(msgPacket);

                    String msg = new String(buf, 4, ByteBuffer.wrap(buf, 0, 4).getInt());
                    Log.d(TAG, "Multicast, from " + msgPacket.getAddress().toString().substring(1) + ":" +
                            msgPacket.getPort() + ", received msg: " + msg);
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}

// o que estava a fazer - como detectar que o ecran se desligou (para iniciar os testes