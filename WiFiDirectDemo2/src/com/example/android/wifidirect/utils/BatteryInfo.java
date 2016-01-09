package com.example.android.wifidirect.utils;

/**
 * Created by DR AT on 26/11/2015
 * .
 */
public class BatteryInfo {
    public int batteryLevel;
    public int batteryScale;
    public int batteryVoltage;
    public int batteryTemperature;

    public BatteryInfo(int batteryLevel, int batteryScale, int batteryVoltage, int batteryTemperature) {
        this.batteryLevel = batteryLevel;
        this.batteryScale = batteryScale;
        this.batteryVoltage = batteryVoltage;
        this.batteryTemperature = batteryTemperature;
    }

    @Override
    public String toString() {
        return "level " + batteryLevel + ", scale " + batteryScale +
                ", voltage " + batteryVoltage/1000.0 + "V, temperature " + batteryTemperature / 10.0 + "ºC";
    }
}
