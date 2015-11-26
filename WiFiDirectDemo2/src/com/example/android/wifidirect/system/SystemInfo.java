package com.example.android.wifidirect.system;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.example.android.wifidirect.ClientActivity;
import com.example.android.wifidirect.WiFiDirectActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by DR AT on 26/11/2015
 * .
 */
public class SystemInfo {

    private static IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

    private static Method getGoToSleepMethod = null;


    static {
        for (Method method : PowerManager.class.getDeclaredMethods()) {
            switch (method.getName()) {
                case "goToSleep":
                    getGoToSleepMethod = method;
                    break;
            }
        }
    }

    /*
     *
     */
    public static BatteryInfo getBatteryInfo(Context context) {
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null)
            return null;

        return new BatteryInfo(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                , batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                , batteryStatus.getIntExtra("voltage", 0)
                , batteryStatus.getIntExtra("temperature", 0));
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