package com.example.android.wifidirect.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Created by AT DR on 08-01-2016
 * .
 */
public class BatteryHelper {

    private boolean running;
    // instancias BatteryManager para lêr a current instantânea
    private BatteryManager batManager;
    // register um broadcast receiver para lêr a voltagem
    private Intent intent;

    /*
     * readBattery
     */
    public void readBattery(Context context) {
        running = true;

        batManager = new BatteryManager();
        intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    logBattery(
//                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
//                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
//                            batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
                            // current instantânea
                            0, //batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
                            0, //batManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER),
                            intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) //voltagem
                    );
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void logBattery(/*int a , int b, int c, */int d, int e, int f) {
    }
}
