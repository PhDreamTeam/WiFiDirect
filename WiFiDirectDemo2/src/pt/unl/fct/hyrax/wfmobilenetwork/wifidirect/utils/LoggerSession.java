package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils;

import android.content.Context;

import java.util.Date;

/**
 * Created by AT DR on 13-01-2016
 * .
 */
public class LoggerSession {
    private Logger mainLog;
    private String sessionName;
    private StringBuilder log = new StringBuilder(20 * 1024);
    private BatteryHelper batteryHelper;

    /**
     *
     */
    public LoggerSession(String sessionName, Logger mainLog) {
        this.mainLog = mainLog;
        this.sessionName = sessionName;
        logMsg(sessionName + "\r\n");
    }

    /**
     * @return returns current time in milliseconds
     */
    public long logTime(String msg) {
        Date now = new Date();
        long nowMs = now.getTime();
        logMsg(msg + ": " + Logger.dateFormatLog.format(now) + ", " + nowMs);
        return nowMs;
    }

    /**
     *
     */
    public void logMsg(String msg) {
        log.append(msg).append("\r\n");
    }

    /**
     *
     */
    public void startLoggingBatteryValues(Context ctx) {
        batteryHelper = new BatteryHelper();
        batteryHelper.startReadingBattery(ctx);
    }

    /**
     *
     */
    public void stopLoggingBatteryValues() {
        batteryHelper.stopReadingBattery();
    }

    /**
     *
     */
    public void logBatteryConsumedJoules() {
        logMsg("Battery info: elapsed time (S): " + String.format("%4.3f", batteryHelper.getRunningTimeMs() / 1000.0) +
                ", consumed joules (J): " + String.format("%3.3f", batteryHelper.getConsumedPowerJoules()));

        logMsg("Battery current values =  " + batteryHelper.getBatteryCurrentValues().toString());

        logMsg("Battery voltage values =  " + batteryHelper.getBatteryVoltageValues().toString());
    }





    /**
     *
     */
    public void close(Context ctx) {
        mainLog.terminateSession(this, ctx);
    }

    /**
     *
     */
    public String getLogData() {
        return log.toString();
    }
}
