package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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
    private Button btnStopFileTask;
    private Button btnStopCpuTask;
    private boolean fileTaskThreadFlag;
    private CpuWorkThread runCPUTaskThread;
    private String systemStat;
    private PowerManager powerManager;
    private Button btnRunBatteryTest;
    private EditText etBatteryTestsTestDuration;
    private EditText etBatteryTestsDelay;
    private EditText etBatteryTestNThreads;
    private int logValuesNumber;
    private Button btnStopBatteryTest;
    private Timer workTimer;
    private ArrayList<CpuWorkThread> workingThreads;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metering_activity);

        context = this;

        batManager = new BatteryManager();

        // used with Notifications ============================================
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // needed for screen off tests
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WFDScreenOFF");

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

        // battery tests ===========================================================
        btnRunBatteryTest = (Button) findViewById(R.id.btnMARunBatteryTest);
        btnStopBatteryTest = (Button) findViewById(R.id.btnMAStopBatteryTest);
        etBatteryTestsTestDuration = (EditText) findViewById(R.id.etMABatteryTestsTestDuration);
        etBatteryTestsDelay = (EditText) findViewById(R.id.etMABatteryTestsDelay);
        etBatteryTestNThreads = (EditText) findViewById(R.id.etMABatteryTestNThreads);

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
                updateProcessInfo();
                updateBatteryInfo(bi);
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

                timer = new Timer("batteryTimer");
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        // BatteryHelper.readBatteryValues(context);
                        // Log.d(TAG, "Battery current_now from file " + counter + " = " + SystemInfo.getBatteryCurrentNowFromFile());
                        final BatteryInfo bi = SystemInfo.getBatteryInfo(context);

                        tvCurrent.post(new Runnable() {
                            @Override
                            public void run() {
                                updateProcessInfo();
                                updateBatteryInfo(bi);
                            }
                        });
                    }
                }, 0, 1000);

                btnBatteryAutoUpdateOn.setVisibility(View.VISIBLE);
            }
        });

        btnRunCpuTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRunCpuTask.setVisibility(View.GONE);
                runCPUTaskThread = createCpuTask();
                runCPUTaskThread.start();
                btnStopCpuTask.setVisibility(View.VISIBLE);
            }
        });

        btnStopCpuTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStopCpuTask.setVisibility(View.GONE);
                runCPUTaskThread.getThread().interrupt();
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

        btnRunBatteryTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int nThreads = Integer.parseInt(etBatteryTestNThreads.getText().toString());
                int testDuration = Integer.parseInt(etBatteryTestsTestDuration.getText().toString());
                int testDelay = Integer.parseInt(etBatteryTestsDelay.getText().toString());

                btnStopBatteryTest.setVisibility(View.VISIBLE);

                testBattery(testDuration * 60, testDelay, nThreads);
            }
        });

        btnStopBatteryTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                endOfBatteryTestActions("Test stopped");
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

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Process task string (from file passed as argument, used in ADB sessions) if exists

        String taskStr = getIntent().getStringExtra("taskStr");
        Log.d(TAG, "TaskStr = " + taskStr);
        if (taskStr != null)
            processTaskStr(taskStr);
    }

    /**
     *
     */
    private void processTaskStr(String taskStr) {
        Log.d(TAG, "TaskString: " + taskStr);
        HashMap<String, String> map = MainActivity.getParamsMap(taskStr);

        String action = map.get("action");
        if (action == null || !(action.equalsIgnoreCase("testBattery")))
            throw new IllegalStateException("Metering activity, received invalid action: " + action);

        if (action.equalsIgnoreCase("testBattery")) {
            // get test Battery params
            int testDurationInMinutes = Integer.parseInt(map.get("testDurationInMinutes"));
            int testRegisterValuesIntervalInSeconds = Integer.parseInt(map.get("testRegisterValuesIntervalInSeconds"));
            int numberOfThreads = Integer.parseInt(map.get("numberOfThreads"));

            // do testBattery
            testBattery(testDurationInMinutes * 60, testRegisterValuesIntervalInSeconds, numberOfThreads);
        }
    }

    /*
     * Charge the battery to 100%, ad let it be charging for more 10 min (just in case).
     * Then disconnect the device and start the test.
     */
    private void testBattery(final int testDurationInSeconds, final int testRegisterValuesIntervalInSeconds,
                             final int numberOfThreads) {
        Log.d(TAG, "Battery test with: | duration = " + (testDurationInSeconds / 60) +
                "m; interval = " + testRegisterValuesIntervalInSeconds + "s; threads = " + numberOfThreads);

        // place to keep working threads
        workingThreads = new ArrayList<>();

        // create timer to register battery and cpu data
        workTimer = new Timer(true);

        // delay in milliseconds
        final int delay = testRegisterValuesIntervalInSeconds * 1000;

        logValuesNumber = 0;

        // read battery level, battery voltage, instantaneous current, battery temperature
        String batteryHeaderValues = BatteryHelper.readBatteryHeaderValues(context);
        Log.d(TAG, "| reading; " + batteryHeaderValues + "; cpu load (%); " + getProcessorsHeader(numberOfThreads));

        // acquire wake lock
        wakeLock.acquire();

        btnRunBatteryTest.setEnabled(false);
        btnRunBatteryTest.setText("Running: 0%");

        // define timer task
        final TimerTask timerTask = new TimerTask() {
            int secondsElapsed = 0;

            public void run() {

                logBatteryAndCpuValues(workingThreads);

                // count minutes passed
                secondsElapsed += testRegisterValuesIntervalInSeconds;
                final int elapsed = secondsElapsed * 100 / testDurationInSeconds;
                btnRunBatteryTest.post(new Runnable() {
                    @Override
                    public void run() {
                        btnRunBatteryTest.setText("Running: " + elapsed + "%");
                    }
                });

                // check for termination
                if (secondsElapsed >= testDurationInSeconds) {
                    // do termination actions
                    endOfBatteryTestActions("End test");
                }
            }
        };

        // get initial cpu values
        systemStat = LinuxUtils.readSystemStat();

        // timer task to start test only after 5 second
        TimerTask initialTimerTask = new TimerTask() {
            @Override
            public void run() {

                // create working threads and keep their references
                for (int i = 0; i < numberOfThreads; i++) {
                    workingThreads.add(createCpuTask());
                }

                // log initial values, zeros
                logBatteryAndCpuValues(workingThreads);

                // start working threads
                for (CpuWorkThread t : workingThreads) {
                    t.start();
                }

                // start timer to fire at fixed rate
                workTimer.scheduleAtFixedRate(timerTask, delay, delay);
            }
        };

        // schedule to init test in 5 seconds, get first values at that time
        workTimer.schedule(initialTimerTask, 5_000);
    }

    /**
     *
     */
    private void endOfBatteryTestActions(String msg) {

        // cancel timer
        workTimer.cancel();

        // stop working threads
        for (CpuWorkThread t : workingThreads) {
            t.getThread().interrupt();
        }

        Log.d(TAG, msg);

        wakeLock.release();
        btnRunBatteryTest.post(new Runnable() {
            @Override
            public void run() {
                btnStopBatteryTest.setVisibility(View.GONE);
                btnRunBatteryTest.setText("Run Battery test");
                btnRunBatteryTest.setEnabled(true);
            }
        });
    }

    /**
     *
     */
    private String getProcessorsHeader(int numberOfThreads) {
        int nProcessors = LinuxUtils.getSystemNumberOfProcessors();
        StringBuilder header = new StringBuilder();

        // get processors header
        for (int i = 0; i < nProcessors; ++i) {
            if (i > 0)
                header.append("; ");

            header.append("cpu").append(i).append("gov; cpu").append(i).append("freq");
        }

        // get threads header
        for (int i = 0; i < numberOfThreads; ++i) {
            header.append("; thread").append(i);
        }

        return header.toString();
    }

    /**
     *
     */
    private String getProcessorsGovernors() {
        int nProcessors = LinuxUtils.getSystemNumberOfProcessors();
        String processorsOnlineString = LinuxUtils.getProcessorOnline();
        String processorsOfflineString = LinuxUtils.getProcessorOffline();
        //Log.d(TAG, "Processors: online -> " + processorsOnlineString + ", offline -> " + processorsOfflineString);

        StringBuilder procGovs = new StringBuilder();

        // for all processors
        for (int i = 0; i < nProcessors; ++i) {
            if (i > 0)
                procGovs.append("; ");

            procGovs.append(LinuxUtils.isProcessorOnline(processorsOnlineString, i) ?
                    LinuxUtils.getProcessorGovernor(i) + "; " + LinuxUtils.getProcessorScalingCurrentFreq(i) :
                    LinuxUtils.isProcessorOffline(processorsOfflineString, i) ? "offline; 0" : "unknown");
        }

        return procGovs.toString();
    }

    /**
     *
     */
    private void logBatteryAndCpuValues(ArrayList<CpuWorkThread> workingThreads) {
        // read battery level, battery voltage, instantaneous current, battery temperature
        String batteryValues = BatteryHelper.readBatteryValues(context);

        // read cpu occupation
        String oldStat = systemStat;
        systemStat = LinuxUtils.readSystemStat();
        float cpuLoad = LinuxUtils.getSystemCpuUsage(oldStat, systemStat);


        // get working thread values
        StringBuilder workingThreadsValues = new StringBuilder();
        for (CpuWorkThread t : workingThreads) {
            workingThreadsValues.append("; ").append(t.getValue());
            t.resetValue();
        }

        Log.d(TAG, "| " + logValuesNumber++ + "; " + batteryValues + "; " + String.format("%.2f", cpuLoad) +
                "; " + getProcessorsGovernors() + workingThreadsValues.toString());
    }

    /**
     *
     */
    public void updateProcessInfo() {
        ProcessInfo pInfo = LinuxUtils.getProcessInfo();
        tvPID.setText("" + pInfo.pid);
        tvPPID.setText("" + pInfo.ppid);
        tvUTime.setText("" + pInfo.utime * 100);
        tvSTime.setText("" + pInfo.stime * 100);
        Log.i(TAG, "Process info -> " + pInfo.getFormattedString());
    }

    public void updateBatteryInfo(BatteryInfo bi) {
        final int batteryCurrentNow = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        tvLevelScale.setText( + bi.batteryLevel + " / " + bi.batteryScale);
        tvVoltage.setText("" + bi.batteryVoltage / 1000.0 + "V");
        tvTemperature.setText("" + bi.batteryTemperature / 10.0 + "ºC");
        tvCurrent.setText("" + batteryCurrentNow);

        Log.i("MeteringActivity", "Battery info -> " + bi);
    }

    /*
     *
     */
    private CpuWorkThread createCpuTask() {
        return new CpuWorkThread();
    }


    /**
     *
     */
    class CpuWorkThread {
        AtomicLong value = new AtomicLong(0);

        Thread thread;

        /**
         *
         */
        public void start() {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    double y = 1.0;
                    for (long n = 0; n < Long.MAX_VALUE; ++n) {
                        // some double work
                        y *= 1.000000000001 + 0.00000000001;
                        if (y > 10000000000000.0)
                            y = 1.0;

                        // increment counter, every few iterations
                        if (n == 1_000) {
                            CpuWorkThread.this.value.getAndIncrement();
                            n = 0;
                        }

                        // check finishing time
                        if (Thread.currentThread().isInterrupted())
                            break;
                    }
                }
            };
            thread = new Thread(runnable);
            thread.start();
        }

        /**
         *
         */
        public long getValue() {
            return value.longValue();
        }

        /**
         *
         */
        public void resetValue() {
            value.set(0);
        }

        /**
         *
         */
        public Thread getThread() {
            return thread;
        }
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
                        //noinspection ResultOfMethodCallIgnored
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

        // acquire a partial wake lock, to stay with cpu running when screen off
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WFDMeterActivity");
        wakeLock.acquire();

        // screen off broadcast receiver
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
        registerReceiver(screenOffBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        // warn user to turn screen off
        tvTurnScreenOFF.setVisibility(View.VISIBLE);

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
     *
     */
    public void doNotificationStart() {
        notify(0, "Test", "Started...", Color.RED, 100, 1000);
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
        notify(0, "Test", "Finished...", Color.RED, 1, 0);
    }


    /*
     * Create and launch a notification with sound
     */
    public void notify(int notificationID, String title, String text, int lightArg, int ledOnMs, int ledOffMs) {

        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.icon)
                // .setSound(soundUri)
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

                //noinspection InfiniteLoopStatement
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