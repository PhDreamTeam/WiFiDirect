package com.example.android.wifidirect.utils;

/**
 * Created by AT DR on 26-11-2015
 * .
 */

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Utilities available only on Linux Operating System.
 * <p/>
 * <p>
 * A typical use is to assign a thread to CPU monitoring:
 * </p>
 * <p/>
 * <pre>
 * public void run() {
 *  while (CpuUtil.monitorCpu) {
 *
 *      LinuxUtils linuxUtils = new LinuxUtils();
 *
 *      int pid = android.os.Process.myPid();
 *      String cpuStat1 = linuxUtils.readSystemStat();
 *      String pidStat1 = linuxUtils.readProcessStat(pid);
 *
 *      try {
 *          Thread.sleep(CPU_WINDOW);
 *      } catch (Exception e) {
 *      }
 *
 *      String cpuStat2 = linuxUtils.readSystemStat();
 *      String pidStat2 = linuxUtils.readProcessStat(pid);
 *
 *      float cpu = linuxUtils.getSystemCpuUsage(cpuStat1, cpuStat2);
 *      if (cpu >= 0.0f) {
 *          _printLine(mOutput, "total";, Float.toString(cpu));
 *      }
 *
 *      String[] toks = cpuStat1.split(");
 *      long cpu1 = linuxUtils.getSystemUptime(toks);
 *
 *      toks = cpuStat2.split("");
 *      long cpu2 = linuxUtils.getSystemUptime(toks);
 *
 *      cpu = linuxUtils.getProcessCpuUsage(pidStat1, pidStat2, cpu2 - cpu1);
 *      if (cpu >= 0.0f) {
 *          _printLine(mOutput, "" + pid, Float.toString(cpu));
 *      }
 *
 *      try {
 *          synchronized (this) {
 *              wait(CPU_REFRESH_RATE);
 *          }
 *      } catch (InterruptedException e) {
 *          e.printStackTrace();
 *          return;
 *      }
 *  }
 *
 *  Log.i("THREAD CPU";, &quot;Finishing&quot;);
 * }
 * </pre>
 */
public final class LinuxUtils {

    // Warning: there appears to be an issue with the column index with android linux:
    // it was observed that on most present devices there are actually
    // two spaces between the 'cpu' of the first column and the value of
    // the next column with data. The thing is the index of the idle
    // column should have been 4 and the first column with data should have index 1.
    // The indexes defined below are coping with the double space situation.
    // If your file contains only one space then use index 1 and 4 instead of 2 and 5.
    // A better way to deal with this problem may be to use a split method
    // not preserving blanks or compute an offset and add it to the indexes 1 and 4.

    private static final int FIRST_SYS_CPU_COLUMN_INDEX = 2;

    private static final int IDLE_SYS_CPU_COLUMN_INDEX = 5;

    /**
     * Return the first line of /proc/stat or null if failed.
     */
    public static String readSystemStat() {

        RandomAccessFile reader = null;
        String load = null;
        try {
            try {
                reader = new RandomAccessFile("/proc/stat", "r");
                load = reader.readLine();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (reader != null)
                    reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return load;
    }

    /**
     * Compute and return the total CPU usage, in percent.
     *
     * @param start first content of /proc/stat. Not null.
     * @param end   second content of /proc/stat. Not null.
     * @return 12.7 for a CPU usage of 12.7% or -1 if the value is not
     * available.
     * @see {@link #readSystemStat()}
     */
    public static float getSystemCpuUsage(String start, String end) {
        String[] stat = start.split("\\s");
        long idle1 = getSystemIdleTime(stat);
        long up1 = getSystemUptime(stat);

        stat = end.split("\\s");
        long idle2 = getSystemIdleTime(stat);
        long up2 = getSystemUptime(stat);

        // don't know how it is possible but we should care about zero and
        // negative values.
        float cpu = -1f;
        if (idle1 >= 0 && up1 >= 0 && idle2 >= 0 && up2 >= 0) {
            if ((up2 + idle2) > (up1 + idle1) && up2 >= up1) {
                cpu = (up2 - up1) / (float) ((up2 + idle2) - (up1 + idle1));
                cpu *= 100.0f;
            }
        }

        return cpu;
    }

    /**
     * Return the sum of uptimes read from /proc/stat.
     *
     * @param stat see {@link #readSystemStat()}
     */
    public static long getSystemUptime(String[] stat) {
        /*
         * (from man/5/proc) /proc/stat kernel/system statistics. Varies with
         * architecture. Common entries include: cpu 3357 0 4313 1362393
         *
         * The amount of time, measured in units of USER_HZ (1/100ths of a
         * second on most architectures, use sysconf(_SC_CLK_TCK) to obtain the
         * right value), that the system spent in user mode, user mode with low
         * priority (nice), system mode, and the idle task, respectively. The
         * last value should be USER_HZ times the second entry in the uptime
         * pseudo-file.
         *
         * In Linux 2.6 this line includes three additional columns: iowait -
         * time waiting for I/O to complete (since 2.5.41); irq - time servicing
         * interrupts (since 2.6.0-test4); softirq - time servicing softirqs
         * (since 2.6.0-test4).
         *
         * Since Linux 2.6.11, there is an eighth column, steal - stolen time,
         * which is the time spent in other operating systems when running in a
         * virtualized environment
         *
         * Since Linux 2.6.24, there is a ninth column, guest, which is the time
         * spent running a virtual CPU for guest operating systems under the
         * control of the Linux kernel.
         */

        // with the following algorithm, we should cope with all versions and
        // probably new ones.
        long l = 0L;

        for (int i = FIRST_SYS_CPU_COLUMN_INDEX; i < stat.length; i++) {
            if (i != IDLE_SYS_CPU_COLUMN_INDEX) { // bypass any idle mode. There is currently only one.
                try {
                    l += Long.parseLong(stat[i]);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                    return -1L;
                }
            }
        }

        return l;
    }

    /**
     * Return the sum of idle times read from /proc/stat.
     *
     * @param stat see {@link #readSystemStat()}
     */
    public static long getSystemIdleTime(String[] stat) {
        try {
            return Long.parseLong(stat[IDLE_SYS_CPU_COLUMN_INDEX]);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }

        return -1L;
    }

    /**
     * Return the first line of /proc/pid/stat or null if failed.
     */
    public static String readProcessStat(int pid) {

        RandomAccessFile reader = null;
        String line = null;

        try {
            try {
                reader = new RandomAccessFile("/proc/" + pid + "/stat", "r");
                line = reader.readLine();
            } finally {
                if (reader != null)
                    reader.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return line;
    }

    /**
     * Compute and return the CPU usage for a process, in percent.
     * <p/>
     * <p>
     * The parameters {@code totalCpuTime} is to be the one for the same period
     * of time delimited by {@code statStart} and {@code statEnd}.
     * </p>
     *
     * @param start  first content of /proc/pid/stat. Not null.
     * @param end    second content of /proc/pid/stat. Not null.
     * @param uptime sum of user and kernel times for the entire system for the
     *               same period of time.
     * @return 12.7 for a cpu usage of 12.7% or -1 if the value is not available
     * or an error occurred.
     * @see {@link #readProcessStat(int)}
     */
    public static float getProcessCpuUsage(String start, String end, long uptime) {

        String[] stat = start.split("\\s");
        long up1 = getProcessUptime(stat);

        stat = end.split("\\s");
        long up2 = getProcessUptime(stat);

        float ret = -1f;
        if (up1 >= 0 && up2 >= up1 && uptime > 0.) {
            ret = 100.f * (up2 - up1) / (float) uptime;
        }

        return ret;
    }

    /**
     * Decode the fields of the file {@code /proc/pid/stat} and return (utime +
     * stime)
     *
     * @param stat obtained with {@link #readProcessStat(int)}
     */
    public static long getProcessUptime(String[] stat) {
        return Long.parseLong(stat[14]) + Long.parseLong(stat[15]);
    }

    /**
     * Decode the fields of the file {@code /proc/pid/stat} and return (cutime +
     * cstime)
     *
     * @param stat obtained with {@link #readProcessStat(int)}
     */
    public static long getProcessIdleTime(String[] stat) {
        return Long.parseLong(stat[16]) + Long.parseLong(stat[17]);
    }

    /**
     * Return the total CPU usage, in percent.
     * <p>
     * The call is blocking for the time specified by elapse.
     * </p>
     *
     * @param elapse the time in milliseconds between reads.
     * @return 12.7 for a CPU usage of 12.7% or -1 if the value is not
     * available.
     */
    public static float syncGetSystemCpuUsage(long elapse) {

        String stat1 = readSystemStat();
        if (stat1 == null) {
            return -1.f;
        }

        try {
            Thread.sleep(elapse);
        } catch (Exception e) {
            e.printStackTrace();
            return -1.f;
        }

        String stat2 = readSystemStat();
        if (stat2 == null) {
            return -1.f;
        }

        return getSystemCpuUsage(stat1, stat2);
    }

    /**
     * Return the CPU usage of a process, in percent.
     * <p>
     * The call is blocking for the time specified by elapse.
     * </p>
     *
     * @param pid
     * @param elapse the time in milliseconds between reads.
     * @return 6.32 for a CPU usage of 6.32% or -1 if the value is not
     * available.
     */
    public static float syncGetProcessCpuUsage(int pid, long elapse) {

        String pidStat1 = readProcessStat(pid);
        String totalStat1 = readSystemStat();
        if (pidStat1 == null || totalStat1 == null) {
            return -1.f;
        }

        try {
            Thread.sleep(elapse);
        } catch (Exception e) {
            e.printStackTrace();
            return -1.f;
        }

        String pidStat2 = readProcessStat(pid);
        String totalStat2 = readSystemStat();
        if (pidStat2 == null || totalStat2 == null) {
            return -1.f;
        }

        String[] toks = totalStat1.split("\\s");
        long cpu1 = getSystemUptime(toks);

        toks = totalStat2.split("\\s");
        long cpu2 = getSystemUptime(toks);

        return getProcessCpuUsage(pidStat1, pidStat2, cpu2 - cpu1);
    }

    /*
     * Show this process info
     */
    public static ProcessInfo getProcessInfo() {
        int pid = android.os.Process.myPid();
        return getProcessInfo(pid);
    }

    /*
     *
     */
    public static ProcessInfo getProcessInfo(int pid) {

        // read process stat file and store their elements in string arrayList
        String procStat = readProcessStat(pid);
        Scanner scan = new Scanner(procStat);
        ArrayList<String> pInfo = new ArrayList<>();
        while (scan.hasNext()) {
            pInfo.add(scan.next());
        }
        scan.close();

        // (1) pid  %d
        // The process ID.
        int thePid = Integer.parseInt(pInfo.get(0));

        //(2) comm  %s
        // The filename of the executable, in parentheses.
        // This is visible whether or not the executable is swapped out.
        String filename = pInfo.get(1);

        // 3) state  %c
        // One of the following characters, indicating process state:
        //  R  Running
        //  S  Sleeping in an interruptible wait
        //  D Waiting in uninterruptible disk sleep
        //  Z Zombie
        //  T Stopped (on a signal)or(before Linux 2.6.33) trace stopped
        //  t Tracing stop(Linux 2.6.33 onward)
        //  W Paging (only before Linux 2.6.0)
        //  X Dead (from Linux 2.6.0 onward)
        //  x Dead (Linux 2.6.33 to 3.13 only)
        //  K Wakekill (Linux 2.6.33 to 3.13 only)
        //  W Waking (Linux 2.6.33 to 3.13 only)
        //  P Parked (Linux 3.9 to 3.13 only)
        char state = pInfo.get(2).charAt(0);

        // (4) ppid % d
        // The PID of the parent of this process.
        int ppid = Integer.parseInt(pInfo.get(3));

        /*
        (5) pgrp % d
        The process group ID of the process.

        (6) session % d
        The session ID of the process.

        (7) tty_nr % d
        The controlling terminal of the process.(The minor
        device number is contained in the combination of
        bits 31 to 20 and 7 to 0;
        the major device number is
        in bits 15 to 8.)

        (8) tpgid % d
        The ID of the foreground process group of the
        controlling terminal of the process.

        (9) flags % u
        The kernel flags word of the process.For bit
        meanings, see the PF_*defines in the Linux kernel
        source file include / linux / sched.h.Details depend
        on the kernel version.

        The format for this field was%lu before Linux 2.6.

        (10) minflt % lu
        The number of minor faults the process has made
        which have not required loading a memory page from
        disk.

        (11) cminflt % lu
        The number of minor faults that the process 's
        waited - for children have made.

        (12) majflt % lu
        The number of major faults the process has made
        which have required loading a memory page from disk.

        (13) cmajflt % lu
        The number of major faults that the process 's
        waited - for children have made.
*/

        // (14) utime % lu
        // Amount of time that this process has been scheduled in user mode,
        // measured in clock ticks (divide by sysconf(_SC_CLK_TCK)).
        // This includes guest time, guest_time(time spent running a virtual CPU, see below),
        // so that applications that are not aware of the guest time field do not lose that
        // time from their calculations.
        int utime = Integer.parseInt(pInfo.get(13));


        // (15) stime % lu
        // Amount of time that this process has been scheduled in kernel mode,
        // measured in clock ticks(divide by sysconf(_SC_CLK_TCK)).
        int stime = Integer.parseInt(pInfo.get(14));

        return new ProcessInfo(pid, filename, state, ppid, utime, stime);
    }

    public static int getSystemClockTickTimeInMs() {
        return 100;
    }


}

