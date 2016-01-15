package com.example.android.wifidirect.utils;

import android.content.Context;
import com.example.android.wifidirect.R;

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

    // TODO build path npt working with com.hyrax.atdr
    static private final String LOG_DIR_PATH = "/storage/sdcard0/Android/data/com.hyrax.atdr/logs";

    private ArrayList<LoggerSession> logSessions = new ArrayList<>();
    private Date initialLogTime;
    private int activeLogSessions = 0;
    private String mainLogName;

    /**
     *
     */
    public Logger(String mainLogName) {
        // ensure log dir path exists
        buildPath(LOG_DIR_PATH);

        this.mainLogName = mainLogName;
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
    public void terminateSession(LoggerSession loggerSession) {
        if (--activeLogSessions == 0) {
            // write all logger sessions to file
            saveDataToFile();
            logSessions.clear();
        }
    }

    /**
     *
     */
    public void saveDataToFile() {
        // get initial time in a nice string
        String timestamp = dateFormatNormal.format(initialLogTime);

        // build file to log data
        final File f = new File(LOG_DIR_PATH + "/WD_" + timestamp + "_log.txt");

        PrintWriter pw = null;

        try {
            pw = new PrintWriter(f);
            pw.println(mainLogName + " started at " + timestamp);
            pw.println();
            for (LoggerSession logSession : logSessions) {
                pw.println("- - - - - - - - - - - -");
                pw.println(logSession.getLogData());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null)
                pw.close();
        }
    }

    /**
     * Creates the received path. The path has to be constructed segment by
     * segment because some segments could have dots and those segment must
     * be create separately from others
     */
    public static void buildPath(String path) {

        String[] components = path.split("/");

        String buildPath = "";
        for (String s : components) {
            if (s.equals("")) {
                if(buildPath.length() != 0)
                    throw new IllegalStateException("Error creating LOG directory: " + buildPath);
                buildPath += '/';
            }
            else {
                if(buildPath.charAt(buildPath.length()-1) != '/')
                    buildPath += '/';
                buildPath += s;
                File logDir = new File(buildPath);
                if (!logDir.mkdir() && !logDir.exists())
                    throw new IllegalStateException("Error creating LOG directory: " + buildPath);
            }
        }
    }


    /**
     * Log also to file
     */
    public static void setLogCatToFile(Context context) {
        String timestamp = dateFormatNormal.format(new Date());

        String appName = context.getResources().getString(R.string.app_name);
        appName = appName.replace(' ', '-');
        String cmd = "logcat -v time -f " + LOG_DIR_PATH + "/logcat_" + timestamp + "_" + appName + ".txt";

        buildPath(LOG_DIR_PATH);

        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
