package pt.unl.fct.hyrax.wfmobilenetwork.accesspoint;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Dual band class.
 *
 * It is not a good idea to use 5GHz as it seems that the wifi hardware is not stable.
 * When I try to activate an AP over 5GHz(selected by device menus) it started to
 * show problems and didn't worked. I didn't try it programmatically.
 */
public class WifiDualBand {

    private static final String TAG = "WifiDualBand";

    private final WifiManager wifiManager;

    private static Method isDualBandSupported;
    private static Method getFrequencyBand;
    private static Method setFrequencyBand;

    static {
        for (Method method : WifiManager.class.getDeclaredMethods()) {
            switch (method.getName()) {
                case "isDualBandSupported":
                    isDualBandSupported = method;
                    break;
                case "getFrequencyBand":
                    getFrequencyBand = method;
                    break;
                case "setFrequencyBand":
                    setFrequencyBand = method;
                    break;
            }
        }
    }

    private WifiDualBand(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }


    /**
     *
     */
    public boolean isDualBandSupported() {
        Object result = invokeQuietly(isDualBandSupported, wifiManager);
        if (result == null) {
            Log.e(TAG, "isDualBandSupported returned error (null)");
            return false;
        }
        return (Boolean) result;
    }

    public static final int WIFI_FREQUENCY_BAND_AUTO = 0;
    public static final int WIFI_FREQUENCY_BAND_5GHZ = 1;
    public static final int WIFI_FREQUENCY_BAND_2GHZ = 2;


    /**
     *
     */
    public int getFrequencyBand() {
        Object result = invokeQuietly(getFrequencyBand, wifiManager);
        if (result == null) {
            Log.e(TAG, "getFrequencyBand returned error (null)");
            return -1;
        }
        return (Integer) result;
    }

    /**
     * @param band
     * @param persist
     */
    public void setFrequencyBand(int band, boolean persist) {
        invokeQuietly(setFrequencyBand, wifiManager, band, persist);
    }

    /*
    *
    */
    private static Object invokeQuietly(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }
}
