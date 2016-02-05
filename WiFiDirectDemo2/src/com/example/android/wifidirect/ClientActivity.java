package com.example.android.wifidirect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.example.android.wifidirect.utils.AndroidUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by DR e AT on 20/05/2015.
 * <p/>
 * <p/>
 * DONE: adapt GUI to collapse transmission or receiving section
 * DONE: enable several receptions simultaneously
 * DONE: controlar o final da transmissão
 * DONE: colocar a velocidade média como delta de inicio e fim, na recepção
 * DONE: controlar os bytes recebidos (pode estar relacionado com o ponto seguinte)
 * porque os bytes recebidos não são iguais aos transmitidos
 * DONE: controlar o fim da recepção: por fim dos dados, ou detecção do fecho do canal
 * DONE: duração da transmissão, na recepção
 * Done: UDP
 * DONE: send image TCP and UDP (not considered as an image)
 * <p/>
 * -
 * <p/>
 * <p/>
 * Frame transmitted TCP & UDP:
 * 4 bytes: address length
 * 4 bytes: buffer number, decreasing and 0 is the last one
 * address string: destination ID; port [;filename]
 */
public class ClientActivity extends Activity {
    public static String TAG = "ClientActivity";

    ClientActivity myThis;

    private Context context;
    private WifiManager wifiManager;

    IStoppable clientTransmitter;
    IStoppable clientReceiver;

    protected int CHOOSE_FILE_RESULT_CODE = 20;
    boolean isTcp;


    private EditText editTextCrIpAddress;
    private EditText editTextCrPortNumber;
    private EditText editTextDestIpAddress;
    private EditText editTextDestPortNumber;
    private EditText editTextTotalBytesToSend;
    private EditText editTextDelay;
    private EditText editTextMaxBufferSize;
    private EditText editTextServerPortNumber;

    private Button btnStartServer, btnStartTransmitting, btnTcp;
    private Button btnRegCrTdls, btnUnRegCrTdls, btnSendImage;
    private Button btnTdls;
    private TextView tvTransmissionZone;
    private TextView tvReceptionZone;
    private LinearLayout llTransmissionZone;
    private LinearLayout llReceptionZone;
    private RadioButton rbReplyInfoNone;
    private RadioButton rbReplyInfoOKs;
    private RadioButton rbReplyInfoEcho;
    private TextView tvTxThrdSentData;
    private TextView tvTxThrdRcvData;
    private RadioButton rbSentKBytes;
    private RadioButton rbSentMBytes;
    private Button btnUdp;
    private Button btnClearReceptionLogs;
    private LinearLayout llReceptionLogs;
    private Button btnStopServer;
    private Button btnStopTransmitting;
    private Button btnStopSendingImage;
    private LinearLayout llTransmissionInputZone;
    private String logDir;

    ArrayList<ReceptionGuiInfo> receptionGuiInfos = new ArrayList<>();

    /*
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        //AndroidUtils.toast(this, "onCreate");
        setContentView(R.layout.client_activity);

        btnStartTransmitting = findViewByIdAndCast(R.id.buttonCAStartTransmitting);
        btnStopTransmitting = findViewByIdAndCast(R.id.buttonCAStopTransmitting);
        btnStartServer = findViewByIdAndCast(R.id.buttonStartServer);
        btnStopServer = findViewByIdAndCast(R.id.buttonStopServer);
        btnSendImage = findViewByIdAndCast(R.id.buttonCASendImage);
        btnStopSendingImage = findViewByIdAndCast(R.id.buttonCAStopSendingImage);
        btnTcp = findViewByIdAndCast(R.id.buttonCATCP);
        btnUdp = findViewByIdAndCast(R.id.buttonCAUDP);

        btnTdls = findViewByIdAndCast(R.id.buttonTdls);
        btnRegCrTdls = findViewByIdAndCast(R.id.buttonRegCrTdls);
        btnUnRegCrTdls = findViewByIdAndCast(R.id.buttonUnRegCrTdls);

        isTcp = btnTcp.getText().toString().equals("TCP");

        // Transmission zone =====================================
        tvTransmissionZone = findViewByIdAndCast(R.id.textViewTransmissionZone);
        llTransmissionZone = findViewByIdAndCast(R.id.LinearLayoutTransmission);
        llTransmissionInputZone = findViewByIdAndCast(R.id.linearLayoutTransmissionInputData);

        editTextCrIpAddress = findViewByIdAndCast(R.id.editTextCrIpAddress);
        editTextCrPortNumber = findViewByIdAndCast(R.id.editTextCrPortNumber);
        editTextDestIpAddress = findViewByIdAndCast(R.id.editTextDestIpAddress);
        editTextDestPortNumber = findViewByIdAndCast(R.id.editTextDestPortNumber);
        editTextTotalBytesToSend = findViewByIdAndCast(R.id.editTextTotalBytesToSend);
        editTextDelay = findViewByIdAndCast(R.id.editTextDelay);
        editTextMaxBufferSize = findViewByIdAndCast(R.id.editTextMaxBufferSize);
        editTextServerPortNumber = findViewByIdAndCast(R.id.editTextServerPortNumber);
        tvTxThrdSentData = findViewByIdAndCast(R.id.textViewCATxThrdSentData);
        tvTxThrdRcvData = findViewByIdAndCast(R.id.textViewCATxThrdRcvData);

        rbSentKBytes = findViewByIdAndCast(R.id.radioButtonCAKBytesToSend);
        rbSentMBytes = findViewByIdAndCast(R.id.radioButtonCAMBytesToSend);

        // reception zone ========================================
        tvReceptionZone = findViewByIdAndCast(R.id.textViewReceptionZone);
        llReceptionZone = findViewByIdAndCast(R.id.LinearLayoutReception);
        llReceptionLogs = findViewByIdAndCast(R.id.linearLayoutReceptionLogs);
        btnClearReceptionLogs = findViewByIdAndCast(R.id.buttonCAClearReceptionLogs);

        rbReplyInfoNone = findViewByIdAndCast(R.id.radioButtonClientReplyInfoNone);
        rbReplyInfoOKs = findViewByIdAndCast(R.id.radioButtonClientReplyInfoOKs);
        rbReplyInfoEcho = findViewByIdAndCast(R.id.radioButtonClientReplyInfoEcho);


        // Remove TDLS buttons on devices that doesn't support it
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Build.VERSION_CODES.KITKAT = API 19
            btnTdls.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Build.VERSION_CODES.LOLLIPOP = API 21
            int addColor = 0xFF006600; // supported color
            if (!isTdlsSupported())
                addColor = 0xFF880000;  // unsupported color
            AndroidUtils.setBtnBackgroundColor(btnTdls, addColor);
        }

        Intent intent = getIntent();
        logDir = intent.getStringExtra("logDir");

        // set listeners on buttons

        tvTransmissionZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                llTransmissionZone.setVisibility(llTransmissionZone.getVisibility() == View.GONE ?
                        View.VISIBLE : View.GONE);
            }
        });

        tvReceptionZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                llReceptionZone.setVisibility(llReceptionZone.getVisibility() == View.GONE ?
                        View.VISIBLE : View.GONE);
            }
        });

        btnStartTransmitting.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startTransmittingGuiActions(false);
                        // start transmitting
                        transmitData(null); // send dummy data for tests
                    }
                });

        btnStopTransmitting.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // stop transmitting
                        clientTransmitter.stopThread();
                        stopTransmittingGuiActions(null);
                    }
                }
        );

        btnSendImage.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startTransmittingGuiActions(true);

                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        AndroidUtils.toast(ClientActivity.this, "Choose image!!");
                    }
                }
        );

        btnStartServer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startReceivingGuiActions();

                        // start receiving
                        startReceiverServer();
                    }
                }
        );

        btnStopServer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // stop receiving server
                        clientReceiver.stopThread();
                        clientReceiver = null;
                        endReceivingGuiActions();
                    }
                }
        );

        btnClearReceptionLogs.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (Iterator<ReceptionGuiInfo> it = receptionGuiInfos.iterator(); it.hasNext(); ) {
                            ReceptionGuiInfo rgi = it.next();
                            if (rgi.isTerminated()) {
                                llReceptionLogs.removeView(rgi.getView());
                                it.remove();
                            }
                        }
                    }
                }
        );

        btnTcp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // change to UDP mode
                        isTcp = false;
                        btnTcp.setVisibility(View.GONE);
                        btnUdp.setVisibility(View.VISIBLE);
                        setEnabledRadioButtonsReplyMode(false);
                        btnSendImage.setEnabled(false);
                    }
                }
        );

        btnUdp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // change to TCP mode
                        isTcp = true;
                        btnUdp.setVisibility(View.GONE);
                        btnTcp.setVisibility(View.VISIBLE);
                        setEnabledRadioButtonsReplyMode(true);
                        btnSendImage.setEnabled(true);
                    }
                }
        );

        btnTdls.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnRegCrTdls.getVisibility() == View.GONE) {
                            btnRegCrTdls.setVisibility(View.VISIBLE);
                            btnUnRegCrTdls.setVisibility(View.VISIBLE);
                        } else {
                            btnRegCrTdls.setVisibility(View.GONE);
                            btnUnRegCrTdls.setVisibility(View.GONE);
                        }
                    }
                }
        );

        btnRegCrTdls.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTdlsEnabled(editTextCrIpAddress.getText().toString(), true);
                    }
                }
        );

        btnUnRegCrTdls.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTdlsEnabled(editTextCrIpAddress.getText().toString(), false);
                    }
                }
        );

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     *
     */
    public void startReceivingGuiActions() {
        setEnabledRadioButtonsReplyMode(false);
        editTextServerPortNumber.setEnabled(false);
        btnStartServer.setVisibility(View.GONE);
        btnStopServer.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    public void endReceivingGuiActions() {
        btnStopServer.setVisibility(View.GONE);
        btnStartServer.setVisibility(View.VISIBLE);
        editTextServerPortNumber.setEnabled(true);
        if (isTcp)
            setEnabledRadioButtonsReplyMode(true);
    }

    /**
     *
     */
    public void startTransmittingGuiActions(boolean sendingFile) {
        if (!sendingFile) {
            btnStartTransmitting.setVisibility(View.GONE);
            btnStopTransmitting.setVisibility(View.VISIBLE);
            btnSendImage.setEnabled(false);
        } else {
            btnSendImage.setVisibility(View.GONE);
            btnStopSendingImage.setVisibility(View.VISIBLE);
            btnStartTransmitting.setEnabled(false);
        }
        setEnabledTransmissionInputViews(false);
    }

    /**
     * @param sourceUri null Sending dummy data, not null sending a file
     */
    public void stopTransmittingGuiActions(Uri sourceUri) {
        if (sourceUri == null) {
            btnStartTransmitting.setVisibility(View.VISIBLE);
            btnStopTransmitting.setVisibility(View.GONE);
            if (isTcp)
                btnSendImage.setEnabled(true);
        } else {
            btnStopSendingImage.setVisibility(View.GONE);
            btnSendImage.setVisibility(View.VISIBLE);
            btnStartTransmitting.setEnabled(true);
        }
        setEnabledTransmissionInputViews(true);
    }

    public String getLogDir() {
        return logDir;
    }

    /**
     * isTdlsSupported
     * needs API 21
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    boolean isTdlsSupported() {
        boolean isTDLSSupported = wifiManager.isTdlsSupported();
        //AndroidUtils.toast(this, "TDLS supported -> " + isTDLSSupported);
        return isTDLSSupported;
    }

    /**
     * setTdlsEnabled
     * need API 19
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    void setTdlsEnabled(String crIpAddressStr, boolean enable) {
        InetAddress remoteIPAddress = null;
        try {
            remoteIPAddress = InetAddress.getByName(crIpAddressStr);
            wifiManager.setTdlsEnabled(remoteIPAddress, enable);
            //AndroidUtils.toast(this, "setTdlsEnabled " + enable + " on " + crIpAddressStr + " with success");

        } catch (UnknownHostException e) {
            Log.e("ClientActivity", "setTdlsEnabled " + enable + " on " + crIpAddressStr, e);
        }
    }

    /**
     * startReceiverServer
     */
    private void startReceiverServer() {
        String rcvPortNumber = editTextServerPortNumber.getText().toString();
        int bufferSize = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

        if (isTcp)
            clientReceiver = new ClientDataReceiverServerSocketThreadTCP(
                    Integer.parseInt(rcvPortNumber), llReceptionLogs, bufferSize, getReplyMode(), this);
        else
            clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                    Integer.parseInt(rcvPortNumber), llReceptionLogs, bufferSize, this);

        clientReceiver.start();
    }

    /**
     * transmitData
     */
    private void transmitData(Uri fileToSend) {
        String crIpAddress = editTextCrIpAddress.getText().toString();
        int crPortNumber = Integer.parseInt(editTextCrPortNumber.getText().toString());
        String destIpAddress = editTextDestIpAddress.getText().toString();
        int destPortNumber = Integer.parseInt(editTextDestPortNumber.getText().toString());
        long totalBytesToSend = Long.parseLong(editTextTotalBytesToSend.getText().toString());
        if (rbSentMBytes.isChecked())
            totalBytesToSend *= 1024;
        long delayMs = Long.parseLong(editTextDelay.getText().toString());
        int bufferSizeBytes = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

        if (isTcp) {
            clientTransmitter = new ClientSendDataThreadTCP(destIpAddress,
                    destPortNumber
                    , crIpAddress, crPortNumber
                    , delayMs, totalBytesToSend
                    , tvTxThrdSentData
                    , tvTxThrdRcvData
                    , this
                    , bufferSizeBytes, fileToSend);
        } else {
            clientTransmitter = new ClientSendDataThreadUDP(destIpAddress,
                    destPortNumber
                    , crIpAddress, crPortNumber
                    , delayMs, totalBytesToSend
                    , tvTxThrdSentData
                    , this
                    , bufferSizeBytes);
        }

        clientTransmitter.start();
    }

    /*
     *
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == RESULT_OK && null != data) {
            Uri uriFileToSend = data.getData();
            Log.d(WiFiDirectActivity.TAG, "Start transmitting image: " + uriFileToSend.toString());
            AndroidUtils.toast(ClientActivity.this, "Start transmitting image: " + uriFileToSend.toString());
            transmitData(uriFileToSend); // send file
        }
    }

    /*
     *
     */
    @Override
    protected void onDestroy() {
        //AndroidUtils.toast(this, "onDestroy");

        if (clientTransmitter != null)
            clientTransmitter.stopThread();

        if (clientReceiver != null)
            clientReceiver.stopThread();

        super.onDestroy();
    }

    /*
     *
     */
    public void onStop() {
        super.onStop();
        //AndroidUtils.toast(this, "onStop");
    }

    /*
     *
     */
    @Override
    protected void onPause() {
        super.onPause();
        //AndroidUtils.toast(this, "onPause");
    }

    /*
     *
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        //AndroidUtils.toast(this, "onRestart");
    }

    /*
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        //AndroidUtils.toast(this, "onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        //AndroidUtils.toast(this, "onStart");
    }

    /*
     *
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AndroidUtils.toast(this, "onBackPressed");
    }

    /*
     *
     */
    public ReplyMode getReplyMode() {
        if (rbReplyInfoOKs.isChecked())
            return ReplyMode.OK;
        if (rbReplyInfoEcho.isChecked())
            return ReplyMode.ECHO;
        return ReplyMode.NONE;
    }

    /*
     *
     */
    private void setEnabledRadioButtonsReplyMode(boolean enable) {
        rbReplyInfoNone.setEnabled(enable);
        rbReplyInfoOKs.setEnabled(enable);
        rbReplyInfoEcho.setEnabled(enable);
    }

    /*
     *
     */
    private void setEnabledTransmissionInputViews(boolean enable) {
        editTextCrIpAddress.setEnabled(enable);
        editTextCrPortNumber.setEnabled(enable);
        editTextDestIpAddress.setEnabled(enable);
        editTextDestPortNumber.setEnabled(enable);
        editTextMaxBufferSize.setEnabled(enable);
        editTextDelay.setEnabled(enable);
        editTextTotalBytesToSend.setEnabled(enable);
        rbSentKBytes.setEnabled(enable);
        rbSentMBytes.setEnabled(enable);
    }

    @SuppressWarnings("unchecked")
    private <T> T findViewByIdAndCast(int id) {
        return (T) findViewById(id);
    }

    public void registerReceptionGuiInfo(ReceptionGuiInfo receptionGuiInfoGui) {
        receptionGuiInfos.add(receptionGuiInfoGui);
    }
}

/*
 *
 */
enum ReplyMode {
    NONE, OK, ECHO
};
