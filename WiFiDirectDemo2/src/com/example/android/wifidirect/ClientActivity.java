package com.example.android.wifidirect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.LightingColorFilter;
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

/**
 * Created by DR e AT on 20/05/2015.
 * <p/>
 * <p/>
 * DONE: adapt GUI to collapse transmission or receiving section
 * DONE: enable several receptions simultaneously
 * DONE: controlar o final da transmiss�o
 * DONE: colocar a velocidade m�dia como delta de inicio e fim, na recep��o
 * DONE: controlar os bytes recebidos (pode estar relacionado com o ponto seguinte)
 * porque os bytes recebidos n�o s�o iguais aos transmitidos
 * DONE: controlar o fim da recep��o: por fim dos dados, ou detec��o do fecho do canal
 * DONE: dura��o da transmiss�o, na recep��o
 * Done: UDP
 * DONE: send image TCP and UDP (not considered as an image)
 * <p/>
 * - gravar resultados em ficheiro - talvez n�o seja necess�rio
 * TODO:  .
 * - place button to close reception result
 * <p/>
 * -
 * <p/>
 */
public class ClientActivity extends Activity {
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

    private Button btnStartStopServer, btnStartStopTransmitting, btnTcp;
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


    // TODO - clear  gui elements that are no more necessary (wait for a while)

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

        btnStartStopTransmitting = (Button) findViewById(R.id.buttonStartStopTransmitting);
        btnStartStopServer = (Button) findViewById(R.id.buttonStartStopServer);
        btnSendImage = (Button) findViewById(R.id.buttonSendImage);
        btnTcp = (Button) findViewById(R.id.buttonCATCP);
        btnUdp = (Button) findViewById(R.id.buttonCAUDP);

        btnTdls = (Button) findViewById(R.id.buttonTdls);
        btnRegCrTdls = (Button) findViewById(R.id.buttonRegCrTdls);
        btnUnRegCrTdls = (Button) findViewById(R.id.buttonUnRegCrTdls);

        isTcp = btnTcp.getText().toString().equals("TCP");

        // Transmission zone =====================================
        tvTransmissionZone = (TextView) findViewById(R.id.textViewTransmissionZone);
        llTransmissionZone = (LinearLayout) findViewById(R.id.LinearLayoutTransmission);

        editTextCrIpAddress = (EditText) findViewById(R.id.editTextCrIpAddress);
        editTextCrPortNumber = (EditText) findViewById(R.id.editTextCrPortNumber);
        editTextDestIpAddress = (EditText) findViewById(R.id.editTextDestIpAddress);
        editTextDestPortNumber = (EditText) findViewById(R.id.editTextDestPortNumber);
        editTextTotalBytesToSend = (EditText) findViewById(R.id.editTextTotalBytesToSend);
        editTextDelay = (EditText) findViewById(R.id.editTextDelay);
        editTextMaxBufferSize = (EditText) findViewById(R.id.editTextMaxBufferSize);
        editTextServerPortNumber = (EditText) findViewById(R.id.editTextServerPortNumber);
        tvTxThrdSentData = (TextView) findViewById(R.id.textViewCATxThrdSentData);
        tvTxThrdRcvData = (TextView) findViewById(R.id.textViewCATxThrdRcvData);

        rbSentKBytes = (RadioButton) findViewById(R.id.radioButtonCAKBytesToSend);
        rbSentMBytes = (RadioButton) findViewById(R.id.radioButtonCAMBytesToSend);

        // reception zone ========================================
        tvReceptionZone = (TextView) findViewById(R.id.textViewReceptionZone);
        llReceptionZone = (LinearLayout) findViewById(R.id.LinearLayoutReception);

        rbReplyInfoNone = (RadioButton) findViewById(R.id.radioButtonClientReplyInfoNone);
        rbReplyInfoOKs = (RadioButton) findViewById(R.id.radioButtonClientReplyInfoOKs);
        rbReplyInfoEcho = (RadioButton) findViewById(R.id.radioButtonClientReplyInfoEcho);


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
            LightingColorFilter lcf = new LightingColorFilter(0xFFFFFFFF, addColor);
            btnTdls.getBackground().setColorFilter(lcf);
        }


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

        btnStartStopTransmitting.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopTransmitting.getText().toString().equals("Start Transmitting")) {
                            transmitData(null); // send dummy data for tests
                            btnStartStopTransmitting.setText("Stop Transmitting");
                            // SystemInfo.goToSleep(ClientActivity.this , ClientActivity.this); // TEST turn off screen
                        } else {
                            // stop transmitting
                            clientTransmitter.stopThread();
                            btnStartStopTransmitting.setText("Start Transmitting");
                        }
                    }
                });

        btnSendImage.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        AndroidUtils.toast(ClientActivity.this, "Choose image!!");
                    }
                });

        btnStartStopServer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopServer.getText().toString().equals("Start Receiving")) {
                            setEnabledRadioButtonsReplyMode(false);
                            btnStartStopServer.setText("Stop Receiving!!!");

                            AndroidUtils.toast(ClientActivity.this, "Start Receiving!!!!!");
                            startReceiverServer();
                        } else {
                            clientReceiver.stopThread();
                            btnStartStopServer.setText("Start Receiving");
                            setEnabledRadioButtonsReplyMode(true);
                        }
                    }
                }

        );

        btnTcp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isTcp = false;
                        btnTcp.setVisibility(View.GONE);
                        btnUdp.setVisibility(View.VISIBLE);
                    }
                }
        );

        btnUdp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isTcp = true;
                        btnUdp.setVisibility(View.GONE);
                        btnTcp.setVisibility(View.VISIBLE);
                    }
                }
        );

        btnTdls.setOnClickListener(new View.OnClickListener() {
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
        });

        btnRegCrTdls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTdlsEnabled(editTextCrIpAddress.getText().toString(), true);
            }
        });

        btnUnRegCrTdls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTdlsEnabled(editTextCrIpAddress.getText().toString(), false);
            }
        });

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
                    Integer.parseInt(rcvPortNumber)
                    , llReceptionZone, bufferSize, getReplyMode());
        else
            clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                    Integer.parseInt(rcvPortNumber)
                    , llReceptionZone, bufferSize);

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

        AndroidUtils.toast(this, "Start transmitting!!!!!");

        if (isTcp) {
            clientTransmitter = new ClientSendDataThreadTCP(destIpAddress,
                    destPortNumber
                    , crIpAddress, crPortNumber
                    , delayMs, totalBytesToSend
                    , tvTxThrdSentData
                    , tvTxThrdRcvData
                    , fileToSend == null ? btnStartStopTransmitting : null
                    , bufferSizeBytes, fileToSend);
        } else {
            clientTransmitter = new ClientSendDataThreadUDP(destIpAddress,
                    destPortNumber
                    , crIpAddress, crPortNumber
                    , delayMs, totalBytesToSend
                    , tvTxThrdSentData
                    , btnStartStopTransmitting
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
}

/*
 *
 */
enum ReplyMode {
    NONE, OK, ECHO
};
