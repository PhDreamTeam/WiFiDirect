package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils;

import android.content.Context;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.MainActivity;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by AT DR on 13-01-2016
 * .
 */
public class Logger {
    static public SimpleDateFormat dateFormatNormal = new SimpleDateFormat("yy-MM-dd_HH'h'mm'm'ss's'");
    static public SimpleDateFormat dateFormatLog = new SimpleDateFormat("HH'h'mm'm'ss's'SSS'ms'");
    static public SimpleDateFormat dateFormatDate = new SimpleDateFormat("yy/MM/dd");

    static private String LOG_DIR_BASE_PATH = MainActivity.APP_MAIN_FILES_DIR_PATH + "/logs";
    static private String LOG_DIR_PATH = LOG_DIR_BASE_PATH;

    private ArrayList<LoggerSession> logSessions = new ArrayList<>();
    private Date initialLogTime;
    private int activeLogSessions = 0;
    private String mainLogName;

    private String deviceName;


    /**
     *
     */
    public Logger(String mainLogName) {
        // ensure log dir path exists
        AndroidUtils.buildPath(LOG_DIR_PATH);

        this.mainLogName = mainLogName;
    }

    /**
     *
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     *
     */
    public LoggerSession getNewLoggerSession(String loggerSessionName) {
        // if first session, get start time
        if (activeLogSessions == 0)
            initialLogTime = new Date();

        LoggerSession ls = new LoggerSession(loggerSessionName, this);
        logSessions.add(ls);
        ++activeLogSessions;
        return ls;
    }

    /**
     *
     */
    public LoggerSession getNewLoggerSession(String loggerSessionName, String logSubDir) {
        if (!(logSubDir == null || logSubDir.trim().equals("") || logSubDir.trim().equals("."))) {
            // ensure log dir path exists
            LOG_DIR_PATH = LOG_DIR_BASE_PATH + "/" + logSubDir;
            AndroidUtils.buildPath(LOG_DIR_PATH);
        }
        return getNewLoggerSession(loggerSessionName);
    }


    /**
     *
     */
    public void terminateSession(LoggerSession loggerSession, Context ctx) {

        saveDataToFile(loggerSession, ctx);

        if (--activeLogSessions == 0) {
            // write all logger sessions to file
            logSessions.clear();
        }
    }


    /**
     * Save all logSessions
     */
    public void saveDataToFile(Context ctx) {
        // get initial time in a nice string
        String timestamp = dateFormatNormal.format(initialLogTime);

        // build file to log data
        final File f = new File(LOG_DIR_PATH + "/log_" + deviceName + "-" + timestamp + "_log.txt");

        PrintWriter pw = null;

        try {
            // println does not put \r
            pw = new PrintWriter(f);
            pw.print(mainLogName + " started at " + timestamp + "\r\n\r\n");
            for (LoggerSession logSession : logSessions) {
                pw.print("- - - - - - - - - - - -" + "\r\n");
                pw.print(logSession.getLogData() + "\r\n");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null)
                pw.close();
        }

        // do a media scan on this file, to enable PC file recognition
        AndroidUtils.executeMediaScanFile(f, ctx);
    }

    /**
     * Save only the logSession received
     */
    public void saveDataToFile(LoggerSession logSession, Context ctx) {
        // get initial time in a nice string
        String timestamp = dateFormatNormal.format(logSession.getInitialLogTime());

        // build file to log data
        final File f = new File(LOG_DIR_PATH + "/log_" + deviceName + "-" + timestamp + "_log.txt");

        PrintWriter pw = null;

        try {
            // println does not put \r
            pw = new PrintWriter(f);
            pw.print(mainLogName + " started at " + timestamp + "\r\n\r\n");
            pw.print("- - - - - - - - - - - -" + "\r\n");
            pw.print(logSession.getLogData() + "\r\n");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null)
                pw.close();
        }

        // do a media scan on this file, to enable PC file recognition
        AndroidUtils.executeMediaScanFile(f, ctx);
    }


    /**
     * Log also to file
     */
    public static File setLogCatToFile(Context context, String deviceName) {
        String timestamp = dateFormatNormal.format(new Date());

        String appName = context.getResources().getString(R.string.app_name);
        appName = appName.replace(' ', '-');
        deviceName = deviceName == null ? "" : deviceName;
        String fileName = LOG_DIR_PATH + "/logcat_" + deviceName + "_" + timestamp + "_" + appName + ".txt";
        String cmd = "logcat -v time -f " + fileName;

        AndroidUtils.buildPath(LOG_DIR_PATH);

        try {
            Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // do a media scan on this file, to enable PC file recognition
        File logCatFile = new File(fileName);
        AndroidUtils.executeMediaScanFile(logCatFile, context);

        return logCatFile;
    }
}
