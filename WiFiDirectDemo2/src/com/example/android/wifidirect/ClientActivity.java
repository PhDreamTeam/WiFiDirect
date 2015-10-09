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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by DR e AT on 20/05/2015.
 * .
 */
public class ClientActivity extends Activity {
    ClientActivity myThis;

    private Context context;
    private WifiManager wifiManager;

    IStoppable clientTransmitter;
    IStoppable clientReceiver;

    protected int CHOOSE_FILE_RESULT_CODE = 20;
    boolean isTcp;

    private EditText editTextRcvThrdRcvData;
    private EditText editTextRcvThrdSentData;
    private EditText editTextCrIpAddress;
    private EditText editTextCrPortNumber;
    private EditText editTextDestIpAddress;
    private EditText editTextDestPortNumber;
    private EditText editTextTotalBytesToSend;
    private EditText editTextDelay;
    private EditText editTextMaxBufferSize;
    private EditText editTextServerPortNumber;
    private EditText editTextTxThrdSentData;
    private EditText editTextTxThrdRcvData;

    private Button btnStartStopServer, btnStartStopTransmitting, btnTcpUdp;
    private Button btnRegCrTdls, btnUnRegCrTdls, btnSendImage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        Toast.makeText(context, "onCreate", Toast.LENGTH_SHORT).show();
        setContentView(R.layout.client_activity);

        btnStartStopTransmitting = (Button) findViewById(R.id.buttonStartStopTransmitting);
        btnStartStopServer = (Button) findViewById(R.id.buttonStartStopServer);
        btnSendImage = (Button) findViewById(R.id.buttonSendImage);
        btnTcpUdp = (Button) findViewById(R.id.buttonTcpUdp);

        isTcp = btnTcpUdp.getText().toString().equals("TCP");
        editTextCrIpAddress = (EditText) findViewById(R.id.editTextCrIpAddress);
        editTextCrPortNumber = (EditText) findViewById(R.id.editTextCrPortNumber);
        editTextDestIpAddress = (EditText) findViewById(R.id.editTextDestIpAddress);
        editTextDestPortNumber = (EditText) findViewById(R.id.editTextDestPortNumber);
        editTextTotalBytesToSend = (EditText) findViewById(R.id.editTextTotalBytesToSend);
        editTextDelay = (EditText) findViewById(R.id.editTextDelay);
        editTextMaxBufferSize = (EditText) findViewById(R.id.editTextMaxBufferSize);

        editTextServerPortNumber = (EditText) findViewById(R.id.editTextServerPortNumber);

        editTextRcvThrdRcvData = (EditText) findViewById(R.id.editTextRcvThrdRcvData);
        editTextRcvThrdSentData = (EditText) findViewById(R.id.editTextRcvThrdSentData);

        editTextTxThrdSentData = (EditText) findViewById(R.id.editTextTxThrdSentData);
        editTextTxThrdRcvData = ((EditText) findViewById(R.id.editTextTxThrdRcvData));


        btnRegCrTdls = (Button) findViewById(R.id.buttonRegCrTdls);
        btnUnRegCrTdls = (Button) findViewById(R.id.buttonUnRegCrTdls);

        // Remove TDLS buttons on devices that doesn't support it
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Build.VERSION_CODES.KITKAT = API 19
            btnRegCrTdls.setVisibility(View.GONE);
            btnUnRegCrTdls.setVisibility(View.GONE);
        }

        // set listeners on buttons

        btnStartStopTransmitting.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopTransmitting.getText().toString().equals("Start Transmitting")) {
                            transmitData(null); // send dummy data for tests
                            btnStartStopTransmitting.setText("Stop Transmitting!!!");
                        } else {
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
                        toast("Choose image!!");
                    }
                });

        btnStartStopServer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopServer.getText().toString().equals("Start Receiving")) {
                            // clear data counter textViewers
                            editTextRcvThrdRcvData.setText("0 KBps");
                            editTextRcvThrdSentData.setText("0 Bytes");

                            String rcvPortNumber = editTextServerPortNumber.getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

                            toast("Start Receiving!!!!!");

                            if (isTcp)
                                clientReceiver = new ClientDataReceiverServerSocketThreadTCP(
                                        Integer.parseInt(rcvPortNumber)
                                        , editTextRcvThrdRcvData
                                        , editTextRcvThrdSentData, bufferSize);
                            else
                                clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                                        Integer.parseInt(rcvPortNumber)
                                        , editTextRcvThrdRcvData, bufferSize);
                            clientReceiver.start();

                            btnStartStopServer.setText("Stop Receiving!!!");
                        } else {
                            clientReceiver.stopThread();
                            btnStartStopServer.setText("Start Receiving");
                        }
                    }
                }

        );

        findViewById(R.id.buttonTcpUdp).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnTcpUdp.getText().toString().equals("TCP")) {
                            btnTcpUdp.setText("UDP");
                            isTcp = false;
                        } else {
                            btnTcpUdp.setText("TCP");
                            isTcp = true;
                        }
                    }
                }
        );

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
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    void setTdlsEnabled(String crIpAddressStr, boolean enable) {
        InetAddress remoteIPAddress = null;
        try {

            remoteIPAddress = InetAddress.getByName(crIpAddressStr);
            wifiManager.setTdlsEnabled(remoteIPAddress, enable);
            toast("setTdlsEnabled " + enable + " on " + crIpAddressStr + " with success");

        } catch (UnknownHostException e) {
            Log.e("ClientActivity", "setTdlsEnabled " + enable + " on " + crIpAddressStr, e);
        }
    }

    void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void transmitData(Uri fileToSend) {
        String crIpAddress = editTextCrIpAddress.getText().toString();
        String crPortNumber = editTextCrPortNumber.getText().toString();
        String destIpAddress = editTextDestIpAddress.getText().toString();
        String destPortNumber = editTextDestPortNumber.getText().toString();
        String totalBytesToSend = editTextTotalBytesToSend.getText().toString();
        String delay = editTextDelay.getText().toString();
        int bufferSize = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

        toast("Start transmitting!!!!!");

        if (isTcp)
            clientTransmitter = new ClientSendDataThreadTCP(destIpAddress,
                    Integer.parseInt(destPortNumber)
                    , crIpAddress, Integer.parseInt(crPortNumber)
                    , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                    , editTextTxThrdSentData
                    , editTextTxThrdRcvData
                    , bufferSize, fileToSend);
        else
            clientTransmitter = new ClientSendDataThreadUDP(destIpAddress,
                    Integer.parseInt(destPortNumber)
                    , crIpAddress, Integer.parseInt(crPortNumber)
                    , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                    , editTextTxThrdSentData
                    , bufferSize); // todo add fileToSend

        clientTransmitter.start();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == RESULT_OK && null != data) {
            Uri uriFileToSend = data.getData();
            Log.d(WiFiDirectActivity.TAG, "Start transmitting image: " + uriFileToSend.toString());
            toast("Start transmitting image: " + uriFileToSend.toString());
            transmitData(uriFileToSend); // send file
        }
    }

    @Override
    protected void onDestroy() {
        toast("onDestroy");

        if (clientTransmitter != null)
            clientTransmitter.stopThread();

        if (clientReceiver != null)
            clientReceiver.stopThread();

        super.onDestroy();
    }

    public void onStop() {
        super.onStop();
        toast("onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        toast("onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        toast("onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        toast("onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        toast("onStart");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        toast("onBackPressed");
    }
}
