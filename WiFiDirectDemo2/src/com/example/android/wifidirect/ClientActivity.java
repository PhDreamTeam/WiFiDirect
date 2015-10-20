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

import java.net.InetAddress;
import java.net.UnknownHostException;

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
 * - gravar resultados em ficheiro - talvez não seja necessário
 * TODO:  .
 * - place button to close reception result
 *
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
    private EditText editTextTxThrdSentData;
    private EditText editTextTxThrdRcvData;

    private Button btnStartStopServer, btnStartStopTransmitting, btnTcpUdp;
    private Button btnRegCrTdls, btnUnRegCrTdls, btnSendImage;
    private Button btnTdls;
    private TextView tvTransmissionZone;
    private TextView tvReceptionZone;
    private LinearLayout llTransmissionZone;
    private LinearLayout llReceptionZone;


    // TODO - clear  gui elements that are no more necessary (wait for a while)


    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        //Toast.makeText(context, "onCreate", Toast.LENGTH_SHORT).show();
        setContentView(R.layout.client_activity);

        btnStartStopTransmitting = (Button) findViewById(R.id.buttonStartStopTransmitting);
        btnStartStopServer = (Button) findViewById(R.id.buttonStartStopServer);
        btnSendImage = (Button) findViewById(R.id.buttonSendImage);
        btnTcpUdp = (Button) findViewById(R.id.buttonTcpUdp);

        btnTdls = (Button) findViewById(R.id.buttonTdls);
        btnRegCrTdls = (Button) findViewById(R.id.buttonRegCrTdls);
        btnUnRegCrTdls = (Button) findViewById(R.id.buttonUnRegCrTdls);

        isTcp = btnTcpUdp.getText().toString().equals("TCP");
        editTextCrIpAddress = (EditText) findViewById(R.id.editTextCrIpAddress);
        editTextCrPortNumber = (EditText) findViewById(R.id.editTextCrPortNumber);
        editTextDestIpAddress = (EditText) findViewById(R.id.editTextDestIpAddress);
        editTextDestPortNumber = (EditText) findViewById(R.id.editTextDestPortNumber);
        editTextTotalBytesToSend = (EditText) findViewById(R.id.editTextTotalBytesToSend);
        editTextDelay = (EditText) findViewById(R.id.editTextDelay);
        editTextMaxBufferSize = (EditText) findViewById(R.id.editTextMaxBufferSize);
        editTextServerPortNumber = (EditText) findViewById(R.id.editTextServerPortNumber);
        editTextTxThrdSentData = (EditText) findViewById(R.id.editTextTxThrdSentData);
        editTextTxThrdRcvData = (EditText) findViewById(R.id.editTextTxThrdRcvData);

        tvTransmissionZone = (TextView) findViewById(R.id.textViewTransmissionZone);
        tvReceptionZone = (TextView) findViewById(R.id.textViewReceptionZone);

        llTransmissionZone = (LinearLayout) findViewById(R.id.LinearLayoutTransmission);
        llReceptionZone = (LinearLayout) findViewById(R.id.LinearLayoutReception);


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
//                            textViewRcvThrdRcvData.setText("0");
//                            textViewRcvThrdSentData.setText("0");

                            String rcvPortNumber = editTextServerPortNumber.getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

                            toast("Start Receiving!!!!!");

                            if (isTcp)
                                clientReceiver = new ClientDataReceiverServerSocketThreadTCP(
                                        Integer.parseInt(rcvPortNumber)
                                        , llReceptionZone, bufferSize);
                            else
                                clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                                        Integer.parseInt(rcvPortNumber)
                                        , llReceptionZone, bufferSize);

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

    // API 21
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    boolean isTdlsSupported() {
        boolean isTDLSSupported = wifiManager.isTdlsSupported();
        //toast("TDLS supported -> " + isTDLSSupported);
        return isTDLSSupported;
    }

    // API 19
    @TargetApi(Build.VERSION_CODES.KITKAT)
    void setTdlsEnabled(String crIpAddressStr, boolean enable) {
        InetAddress remoteIPAddress = null;
        try {
            remoteIPAddress = InetAddress.getByName(crIpAddressStr);
            wifiManager.setTdlsEnabled(remoteIPAddress, enable);
            //toast("setTdlsEnabled " + enable + " on " + crIpAddressStr + " with success");

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
                    , fileToSend == null ? btnStartStopTransmitting : null
                    , bufferSize, fileToSend);
        else
            clientTransmitter = new ClientSendDataThreadUDP(destIpAddress,
                    Integer.parseInt(destPortNumber)
                    , crIpAddress, Integer.parseInt(crPortNumber)
                    , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                    , editTextTxThrdSentData
                    , btnStartStopTransmitting
                    , bufferSize);

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
        //toast("onDestroy");

        if (clientTransmitter != null)
            clientTransmitter.stopThread();

        if (clientReceiver != null)
            clientReceiver.stopThread();

        super.onDestroy();
    }

    public void onStop() {
        super.onStop();
        //toast("onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //toast("onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //toast("onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //toast("onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        //toast("onStart");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        toast("onBackPressed");
    }
}
