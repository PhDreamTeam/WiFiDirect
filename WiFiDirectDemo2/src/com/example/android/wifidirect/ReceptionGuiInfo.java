package com.example.android.wifidirect;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    public ReceptionGuiInfo(final LinearLayout parentLinearLayout, final String ipAddress, final int localPort) {
        this.parentLinearLayout = parentLinearLayout;
        context = parentLinearLayout.getContext();
        this.ipAddress = ipAddress.substring(1);
        this.localPort = localPort;

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

}
