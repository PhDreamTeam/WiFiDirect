package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

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

    public static String readBatteryHeaderValues(Context context) {
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null)
            throw new IllegalStateException("Battery helper can't register ACTION_BATTERY_CHANGED intent");

        int batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);

        return "Current now; current average; energy; charge counter; voltage (V); capacity (%); level/" +
                batteryScale + "; temperature (ºc); health";
    }


    /*
     * read battery changeable values
     */
    public static String readBatteryValues(Context context) {

        final BatteryManager batManager = new BatteryManager();
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null)
            throw new IllegalStateException("Battery helper can't register ACTION_BATTERY_CHANGED intent");

        // read battery values and register them

        int batteryCurrentNow = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        int batteryCurrentAverage = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);

        int batteryChargeCounter = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        int batteryEnergy = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);

        int batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int batteryCapacity = batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        int batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        String batteryHealth = batteryHealthToString(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0));

        return "" + batteryCurrentNow + "; " + batteryCurrentAverage + "; " + batteryEnergy +
                "; " + batteryChargeCounter + "; " + String.format("%.3f", batteryVoltage / 1000.0) +
                "; " + batteryCapacity + "; " + batteryLevel +
                "; " + String.format("%.1f", batteryTemperature / 10.0) + "; " + batteryHealth;
    }

    /**
     *
     */
    private static String batteryHealthToString(int intExtraBatteryHealth) {
        String[] healthValues = {"ERROR", "BATTERY_HEALTH_UNKNOWN", "BATTERY_HEALTH_GOOD", "BATTERY_HEALTH_OVERHEAT",
                "BATTERY_HEALTH_DEAD", "BATTERY_HEALTH_OVER_VOLTAGE", "BATTERY_HEALTH_UNSPECIFIED_FAILURE",
                "BATTERY_HEALTH_COLD"};
        return healthValues[intExtraBatteryHealth];
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
