package com.example.android.wifidirect.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by AT DR on 26-11-2015
 * .
 */
public class AndroidUtils {

    /*
     * Make a short toast
     */
    public static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
}
