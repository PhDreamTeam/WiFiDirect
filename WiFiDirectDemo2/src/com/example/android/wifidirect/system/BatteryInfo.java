package com.example.android.wifidirect.system;

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
}
