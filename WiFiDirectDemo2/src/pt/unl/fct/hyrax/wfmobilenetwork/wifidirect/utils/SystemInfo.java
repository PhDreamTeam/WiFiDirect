package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.WiFiDirectActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Scanner;

/**
 * Created by DR AT on 26/11/2015
 * .
 */
public class SystemInfo {

    private static IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private static Intent batteryStatusIntent = null;

    // unnecessary - from here
//    private static Method getGoToSleepMethod = null;
//
//    static {
//        for (Method method : PowerManager.class.getDeclaredMethods()) {
//            switch (method.getName()) {
//                case "goToSleep":
//                    getGoToSleepMethod = method;
//                    break;
//            }
//        }
//    }
    // to here


    /*
     *
     */
    public static BatteryInfo getBatteryInfo(Context context) {

        batteryStatusIntent = context.registerReceiver(null, iFilter);
        if (batteryStatusIntent == null)
            throw new RuntimeException("registerReceiver returned null");


        return new BatteryInfo(
                batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                , batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                , batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                , batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
    }

    /*
     *
     */
    public static int getBatteryCurrentNowFromFile() {
        try {
            Scanner scan = new Scanner(new File("/sys/class/power_supply/battery/current_now"));
            int current_now = scan.nextInt();
            scan.close();
            return current_now;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return -1000000000;
    }

    /*
     *
     */
    private static Object invokeQuietly(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e(WiFiDirectActivity.TAG, "Error on SystemInfo", e);
        }
        return null;
    }

    /**
     * Problem: the mac address reported by WifiP2PDevice are not the ones watched in arp tables
     * WifiP2PDevice is 00:00:00:00:80:00 more than ARP tables. The +8 is circular.
     */
    public static String getIPFromMac(String macAddress) {

        String[] parts = macAddress.split(":");
//        if (parts[4].charAt(0) != '0'){
//            parts[4] = "8"  + parts[4].charAt(1);
//        } else {
//            if (parts[4].charAt(0) < '8')
//                throw new IllegalStateException("The MAC address, to be searched in ARP file, " +
//                        "don't have the expected values - see commented bug: " + macAddress);

        // subtract 8 to that digit
        int n = Character.digit(parts[4].charAt(0), 16);
        parts[4] = "" + Character.forDigit((n + 8) % 16, 16) + parts[4].charAt(1);
//    }

        // build new mac address
        macAddress = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":" + parts[4] + ":" + parts[5];

        Scanner scan = null;
        try {
            scan = new Scanner(new File("/proc/net/arp"));
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                Scanner tokens = new Scanner(line);
                String ip = tokens.next();
                tokens.next();
                tokens.next();
                String mac = tokens.next();
                if (mac.equalsIgnoreCase(macAddress)) {
                    return ip;
                }
                tokens.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (scan != null)
                scan.close();
        }
        return null;
    }

    /**
     *
     */
    public static String getInterfaceMacAddress(String interfaceName) {
        try {

            NetworkInterface ntwInterface = NetworkInterface.getByName(interfaceName);
            if (ntwInterface == null)
                return null;
            return getMACStringFromBytes(ntwInterface.getHardwareAddress());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     */
    public static String getMACStringFromBytes(byte[] macBytes) {
        if (macBytes == null)
            return null;
        // get hardware address from interface
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < macBytes.length; i++) {
            strBuilder.append(String.format("%02x" + (i < macBytes.length - 1 ? ":" : ""), macBytes[i]));
        }
        return strBuilder.toString();
    }


}

//    public static void goToSleep(Context context, Activity act) {
// version 1 NOT WORKING
//        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        invokeQuietly(getGoToSleepMethod, mPowerManager, SystemClock.uptimeMillis(), 0, 0 );

// version 2 NOT WORKING
//Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10);
//        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10_000);

// version 3 NOT WORKING
//        ComponentName compName = new ComponentName(context, MyAdmin.class);
//        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
//        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Turning display off...");
//        act.startActivityForResult(intent, 1);
//
//
//        DevicePolicyManager deviceManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
//        deviceManager.lockNow();
//    }

// Sets screenOn:
//    public void setScreenOn(final boolean screenOn, Activity activity) {
//        PowerManager mPowerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
//
//
//        if(mPowerManager == null) {
//            Log.e(WiFiDirectActivity.TAG, "==setScreenOnOff: mPowerManager == null");
//            return;
//        }
//
//        if(screenOn) {
//            mPowerManager.wakeUp(SystemClock.uptimeMillis());
//        } else {
//            mPowerManager.goToSleep(SystemClock.uptimeMillis());
//        }


//
//class MyAdmin extends DeviceAdminReceiver {
//
//
//    static SharedPreferences getSamplePreferences(Context context) {
//        return context.getSharedPreferences(
//                DeviceAdminReceiver.class.getName(), 0);
//    }
//
//    static String PREF_PASSWORD_QUALITY = "password_quality";
//    static String PREF_PASSWORD_LENGTH = "password_length";
//    static String PREF_MAX_FAILED_PW = "max_failed_pw";
//
//    void showToast(Context context, CharSequence msg) {
//        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void onEnabled(Context context, Intent intent) {
//        showToast(context, "Sample Device Admin: enabled");
//    }
//
//    @Override
//    public CharSequence onDisableRequested(Context context, Intent intent) {
//        return "This is an optional message to warn the user about disabling.";
//    }
//
//    @Override
//    public void onDisabled(Context context, Intent intent) {
//        showToast(context, "Sample Device Admin: disabled");
//    }
//
//    @Override
//    public void onPasswordChanged(Context context, Intent intent) {
//        showToast(context, "Sample Device Admin: pw changed");
//    }
//
//    @Override
//    public void onPasswordFailed(Context context, Intent intent) {
//        showToast(context, "Sample Device Admin: pw failed");
//    }
//
//    @Override
//    public void onPasswordSucceeded(Context context, Intent intent) {
//        showToast(context, "Sample Device Admin: pw succeeded");
//    }
//
//}