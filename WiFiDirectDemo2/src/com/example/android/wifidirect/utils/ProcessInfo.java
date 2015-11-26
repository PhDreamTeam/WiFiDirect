package com.example.android.wifidirect.utils;

/**
 * Created by AT DR on 26-11-2015
 * .
 */
public class ProcessInfo {
    int pid, ppid, utime, stime;
    char state;
    String filename;

    public ProcessInfo(int pid, String filename, char state, int ppid, int utime, int stime) {
        this.filename = filename;
        this.pid = pid;
        this.ppid = ppid;
        this.state = state;
        this.stime = stime;
        this.utime = utime;
    }

    /*
     *
     */
    public String getFormattedString() {
        StringBuilder s = new StringBuilder();
        s.append("pid = ").append(pid).append("\n");
        s.append("filename = ").append(filename).append("\n");
        s.append("state = ").append(state).append("\n");
        s.append("ppid = ").append(ppid).append("\n");
        s.append("utime = ").append(utime).append(" clock ticks, ").append(
                (utime * LinuxUtils.getSystemClockTickTimeInMs())).append("ms").append("\n");
        s.append("stime = ").append(stime).append(" clock ticks, ").append(
                (stime * LinuxUtils.getSystemClockTickTimeInMs())).append("ms");
        return s.toString();
    }
}
