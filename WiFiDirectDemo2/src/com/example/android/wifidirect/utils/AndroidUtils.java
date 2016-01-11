package com.example.android.wifidirect.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Toast;

/**
 * Created by AT DR on 26-11-2015
 * .
 */
public class AndroidUtils {

    /*
     * Make a toast
     */
    public static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    /*
     * Make a toast, to be used from a background thread
     */
    public static void toast(final View view, final String msg) {
        view.post(new Runnable() {
            @Override
            public void run() {
                toast(view.getContext(), msg);
            }
        });
    }

    /*
    *
    */
    public static void showDialog(Context context, String tittle, String msg, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(tittle).setMessage(msg).setCancelable(false);
        builder.setNeutralButton(android.R.string.ok, onClickListener).show();
    }

}
