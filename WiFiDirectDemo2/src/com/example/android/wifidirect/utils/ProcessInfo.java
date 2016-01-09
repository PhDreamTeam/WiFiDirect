package com.example.android.wifidirect.utils;

/**
 * Created by AT DR on 26-11-2015
 * .
 */
public class ProcessInfo {
    public int pid, ppid, utime, stime;
    public char state;
    public String filename;

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
        s.append("pid/ppid = ").append(pid).append("/").append(ppid);
       // s.append("filename = ").append(filename).append("\n");
        s.append(", state = ").append(state);
        s.append(", utime = ").append(utime).append(" cticks, ").append(
                (utime * LinuxUtils.getSystemClockTickTimeInMs())).append(" ms");
        s.append(", stime = ").append(stime).append(" cticks, ").append(
                (stime * LinuxUtils.getSystemClockTickTimeInMs())).append(" ms");
        return s.toString();
    }
}
