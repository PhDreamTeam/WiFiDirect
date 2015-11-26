package com.example.android.wifidirect;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ateofilo on 19-10-2015.
 * .
 */
class ReceptionGuiInfo {
    LinearLayout parentLinearLayout;
    LinearLayout thisLinearLayout;
    Context context;
    String ipAddress;
    int localPort;

    private TextView tvReceivedData;
    private TextView tvSentData;
    private TextView tvMaxRcvSpeed;
    private TextView tvCurAvgRcvSpeed;
    private TextView tvLabel;

    ArrayList<DataTransferInfo> transferInfoArrayList;

    //debug
    ClientDataReceiverServerSocketThreadTCP.ClientDataReceiverThreadTCP clientDataReceiverThreadTCP;

    public ReceptionGuiInfo(final LinearLayout parentLinearLayout, final String ipAddress, final int localPort
            , final ArrayList<DataTransferInfo> transferInfoArrayList
            , final ClientDataReceiverServerSocketThreadTCP.ClientDataReceiverThreadTCP cliThread) {
        this.parentLinearLayout = parentLinearLayout;
        context = parentLinearLayout.getContext();
        this.ipAddress = ipAddress.substring(1);
        this.localPort = localPort;
        this.transferInfoArrayList = transferInfoArrayList;
        this.clientDataReceiverThreadTCP = cliThread;

        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

                // main linear layout
                thisLinearLayout = new LinearLayout(context);
                thisLinearLayout.setOrientation(LinearLayout.VERTICAL);
                thisLinearLayout.setLayoutParams(params);

                // space text view
                TextView tvSpace = new TextView(context);
                tvSpace.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 7));
                thisLinearLayout.addView(tvSpace);

                // id text view
                tvLabel = new TextView(context);
                tvLabel.setText(
                        "Rec: " + ReceptionGuiInfo.this.ipAddress + " at port " + ReceptionGuiInfo.this.localPort);
                tvLabel.setBackgroundColor(0xff285523);
                tvLabel.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                tvLabel.setGravity(Gravity.CENTER);
                thisLinearLayout.addView(tvLabel, params);
                tvLabel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final StringBuilder sb = new StringBuilder(500);
                        final StringBuilder sbDetailed = new StringBuilder(500);
                        //add current system time
                        sb.append("Speed(Mbps), dT(s), dB(MB)\n");
                        sbDetailed.append("Speed(Mbps), dT(s), dB(MB)\n");
                        for (DataTransferInfo dti : transferInfoArrayList) {
                            if (dti != null) {
                                sb.append(dti);
                                sbDetailed.append(dti.toStringDetailed());
                            } else {
                                sb.append("Totals:");
                                sbDetailed.append("Totals:");
                            }
                            sb.append("\n");
                            sbDetailed.append("\n");
                        }
                        if (cliThread != null && cliThread.batteryInitial != null) {
                            String initS = "Battery init: Level=" + cliThread.batteryInitial.batteryLevel +
                                    ", pct=" + (cliThread.batteryInitial.batteryLevel / (double) cliThread.batteryInitial.batteryScale) * 100.0 + "%" +
                                    ", voltage=" + cliThread.batteryInitial.batteryVoltage + "mV, temp=" + cliThread.batteryInitial.batteryTemperature / 10.0 + "ºC\n";
                            sb.append(initS);
                            sbDetailed.append(initS);
                        }
                        if (cliThread != null && cliThread.batteryFinal != null) {
                            String finalS = "Battery Final: Level=" + cliThread.batteryFinal.batteryLevel +
                                    ", pct=" + (cliThread.batteryFinal.batteryLevel / (double)cliThread.batteryFinal.batteryScale)  * 100.0 + "%" +
                                    ", voltage=" + cliThread.batteryFinal.batteryVoltage+ "mV, temp=" + cliThread.batteryFinal.batteryTemperature / 10.0 + "ºC";
                            sb.append(finalS);
                            sbDetailed.append(finalS);
                        }
                        showYesNoDialog(context, "Transfer Information", sb.toString()
                                , R.string.Save, android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Save on file the transfer Data
                                        saveTransferInfoOnFile(sbDetailed.toString(), "transferData.txt");
                                    }
                                }, null);

                    }
                });

                // ====================================================
                // results line 1
                LinearLayout ll1 = new LinearLayout(context);
                ll1.setOrientation(LinearLayout.HORIZONTAL);
                ll1.setLayoutParams(params);

                TextView tv1_ll1 = new TextView(context);
                tv1_ll1.setText("Rcv Data (KB): ");
                ll1.addView(tv1_ll1);

                tvReceivedData = new TextView(context);
                tvReceivedData.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                tvReceivedData.setText("0000");
                ll1.addView(tvReceivedData);

                TextView tv2_ll1 = new TextView(context);
                tv2_ll1.setText(" /  Sent Data (B): ");
                ll1.addView(tv2_ll1);

                tvSentData = new TextView(context);
                tvSentData.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                tvSentData.setText("0000");
                ll1.addView(tvSentData);

                thisLinearLayout.addView(ll1);

                // ====================================================
                // results line 2
                LinearLayout ll2 = new LinearLayout(context);
                ll2.setOrientation(LinearLayout.HORIZONTAL);
                ll2.setLayoutParams(params);

                TextView tv1_ll2 = new TextView(context);
                tv1_ll2.setText("Speed (Mbps):  Max: ");
                ll2.addView(tv1_ll2);

                tvMaxRcvSpeed = new TextView(context);
                tvMaxRcvSpeed.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                tvMaxRcvSpeed.setText("0000");
                ll2.addView(tvMaxRcvSpeed);

                TextView tv2_ll2 = new TextView(context);
                tv2_ll2.setText(" /  Avg: ");
                ll2.addView(tv2_ll2);

                tvCurAvgRcvSpeed = new TextView(context);
                tvCurAvgRcvSpeed.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                tvCurAvgRcvSpeed.setText("0000");
                ll2.addView(tvCurAvgRcvSpeed);

                thisLinearLayout.addView(ll2);

                parentLinearLayout.addView(thisLinearLayout);
            }
        });
    }

    private void saveTransferInfoOnFile(String transferInfoStr, String filename) {
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd_HH'h'mm'm'ss's'");
        String timestamp = sdf.format(new Date());

        final File f = new File(Environment.getExternalStorageDirectory() + "/"
                + context.getPackageName() + "/" + timestamp + "_" + filename); // add filename
        File dirs = new File(f.getParent());
        if (!dirs.exists())
            dirs.mkdirs();
        //f.createNewFile();
        Log.d(WiFiDirectActivity.TAG, "TransferInfoData: saving to file: " + f.toString());
        PrintWriter fopw = null;
        try {
            fopw = new PrintWriter(f);
            fopw.println(timestamp + ", " + filename);
            fopw.print(transferInfoStr);

        } catch (FileNotFoundException e) {
            Log.d(WiFiDirectActivity.TAG, "Error TransferInfoData: File not Found: " + f.toString());
        } finally {
            if (fopw != null)
                fopw.close();
        }
    }

    public String getTvCurAvgRcvSpeed() {
        return tvCurAvgRcvSpeed.getText().toString();
    }

    public String getTvMaxRcvSpeed() {
        return tvMaxRcvSpeed.getText().toString();
    }

    public String getTvReceivedData() {
        return tvReceivedData.getText().toString();
    }

    public String getTvSentData() {
        return tvSentData.getText().toString();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setTvReceivedData(final long receivedData) {
        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                ReceptionGuiInfo.this.tvReceivedData.setText(Long.toString(receivedData));
            }
        });
    }

    public void setTvSentData(final long sentData) {
        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                ReceptionGuiInfo.this.tvSentData.setText(Long.toString(sentData));
            }
        });
    }

    public void setMaxRcvSpeed(final double maxRcvSpeed) {
        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                ReceptionGuiInfo.this.tvMaxRcvSpeed.setText(String.format("%4.3f", maxRcvSpeed * 8 / 1024));
            }
        });
    }

    public void setCurAvgRcvSpeed(final double curAvgRcvSpeed) {
        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                ReceptionGuiInfo.this.tvCurAvgRcvSpeed.setText(String.format("%4.3f", curAvgRcvSpeed * 8 / 1024));
            }
        });
    }

    public void setData(final long receivedData, final long sentData, final double maxRcvSpeed, final double curAvgRcvSpeed) {
        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                ReceptionGuiInfo.this.tvReceivedData.setText(Long.toString(receivedData));
                ReceptionGuiInfo.this.tvSentData.setText(Long.toString(sentData));
                ReceptionGuiInfo.this.tvMaxRcvSpeed.setText(String.format("%4.3f", maxRcvSpeed * 8 / 1024));
                ReceptionGuiInfo.this.tvCurAvgRcvSpeed.setText(String.format("%4.3f", curAvgRcvSpeed * 8 / 1024));
            }
        });
    }

    public void setTerminatedState() {

        parentLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                tvLabel.setBackgroundColor(0xF721391E);
                tvLabel.setTextAppearance(context, android.R.style.TextAppearance_Small);
            }
        });
    }

    /*
     *
     */
    public static void showYesNoDialog(Context context, String tittle, String msg
            , int yesResourceId, int noResourceId, DialogInterface.OnClickListener onClickYesListener, DialogInterface.OnClickListener onClickNoListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(tittle).setMessage(msg).setCancelable(false);
        builder.setNegativeButton(noResourceId, onClickNoListener);
        builder.setPositiveButton(yesResourceId, onClickYesListener);
        builder.show();
    }

}
