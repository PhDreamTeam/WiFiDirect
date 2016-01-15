package com.example.android.wifidirect.utils;

import java.util.Date;

/**
 * Created by AT DR on 13-01-2016
 * .
 */
public class LoggerSession {
    private Logger mainLog;
    private String sessionName;
    private StringBuilder log = new StringBuilder(20 * 1024);

    /**
     *
     */
    public LoggerSession(String sessionName, Logger mainLog) {
        this.mainLog = mainLog;
        this.sessionName = sessionName;
        log.append(sessionName).append("\n\n");
    }

    /**
     *
     * @return returns current time in milliseconds
     */
    public long logTime(String msg) {
        Date now = new Date();
        long nowMs =  now.getTime();
        logMsg(msg + ": " + Logger.dateFormatLog.format(now) + ", " + nowMs);
        return nowMs;
    }

    /**
     *
     */
    public void logMsg(String msg) {
        log.append(msg).append("\n");
    }

    /**
     *
     */
    public void close() {
        mainLog.terminateSession(this);
    }

    /**
     *
     */
    public String getLogData() {
        return log.toString();
    }
}
