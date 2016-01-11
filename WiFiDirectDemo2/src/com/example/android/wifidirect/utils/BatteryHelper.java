package com.example.android.wifidirect.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by AT DR on 08-01-2016
 * .
 */
public class BatteryHelper {

    private boolean running;

    private long accumulatedCurrentPlusVoltage = 0;
    private long startTimeMs;
    private long endTimeMs;
    private Thread workThread;


    /*
     *
     */
    public void startReadingBattery(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startReadingBatteryApiLollipop(context);
        } else {
            // ... TODO
        }
    }


    /*
     * readBattery
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startReadingBatteryApiLollipop(Context context) {
        running = true;

        final BatteryManager batManager = new BatteryManager();
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null)
            throw new IllegalStateException("Battery helper can't register ACTION_BATTERY_CHANGED intent");

        workThread = new Thread(new Runnable() {
            @Override
            public void run() {

                startTimeMs = System.currentTimeMillis();

                while (running) {
                    // read battery values and register them
                    logBattery(
                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER),
                            intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    );

                    // wait for one second, to read again
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                endTimeMs = System.currentTimeMillis();

                logFinalValues();
            }
        });

        workThread.start();
    }

    /*
     * Blocking method
     */
    public void stopReadingBattery() {
        running = false;

        // wait end of thread
        try {
            workThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /*
     *
     */
    private void logFinalValues() {
        Log.i("Battery helper: ", "Running time (ms) = " + getRunningTimeMs() +
                ", consumed power (mcWh) = " + getConsumedPowerMicroWh());
    }

    /*
     *
     */
    public long getRunningTimeMs() {
        return endTimeMs - startTimeMs;
    }

    /*
     *
     */
    public double getConsumedPowerMicroWh() {
        return accumulatedCurrentPlusVoltage / (1000.0 * 3600);
    }

    /*
     *
     */
    public double getConsumedPowerJoules() {
        return accumulatedCurrentPlusVoltage / (1000.0 * 1000 * 1000);
    }

    /*
     * voltage in mlV, current in mcA
     *
     * Pot = V * I = (mlV / 1000) * (mcA / 1000*1000)
     * E = pot * t = pot * 1 / 3600
     *
     * e(Wh) = ( V * I ) / (1000 * 1000 * 1000 * 3600) + ( V * I ) / (1000 * 1000 * 1000 * 3600) + ( V * I ) / (1000 * 1000 * 1000 * 3600)
     *
     * e(Wh) = ((V1 * I1) + (V2 * I2) + ... + (Vn * In)) / (1000 * 1000 * 1000 * 3600)
     *
     * e(J) = e(Wh) * 3600   // 1 Wh is 3.6×10^3 J = 3.6 kJ = 3600 J
     */
    private void logBattery(int capacity, int chargeCounter, int currentAverage, int currentNow, int energyCounter,
                            int voltageNow) {
        Log.i("Battery helper: ",
                "capacity = " + capacity + ", current now = " + currentNow + ", voltage now = " + voltageNow);
        accumulatedCurrentPlusVoltage += currentNow * voltageNow;
    }
}
