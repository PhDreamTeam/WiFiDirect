package com.example.android.wifidirect.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.LightingColorFilter;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

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

    /*
     *
     */
    public static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

    /*
     * Not correct
     */
    public static void setBtnBackgroundColor(Button btn, int bgColor) {
        LightingColorFilter lcf = new LightingColorFilter(0xFFFFFFFF, bgColor);
        btn.getBackground().setColorFilter(lcf);
    }

    /*
    *
    */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    /*
     *
     */
    public static NetworkInterface getNetworkInterface(String interfaceName) {
        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (netInterface.getName().equalsIgnoreCase(interfaceName)) {
                    return netInterface;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

}
