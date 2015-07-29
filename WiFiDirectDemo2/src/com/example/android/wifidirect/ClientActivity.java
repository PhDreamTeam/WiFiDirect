package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by DR e AT on 20/05/2015.
 *
 */
public class ClientActivity extends Activity {
    ClientActivity myThis;
    IStopable clientTransmiter;
    IStopable clientReceiver;

    Button btnStartStopServer, btnStartStopTransmitting, btnTcpUdp;
    boolean isTcp;
    protected int CHOOSE_FILE_RESULT_CODE = 20;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);
        Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_SHORT).show();
        setContentView(R.layout.client_activity);
        btnStartStopTransmitting = (Button) findViewById(R.id.buttonStartStopTransmitting);
        btnStartStopServer = (Button) findViewById(R.id.buttonStartStopServer);
        btnTcpUdp = (Button) findViewById(R.id.buttonTcpUdp);

        isTcp = btnTcpUdp.getText().toString().equals("TCP");


        findViewById(R.id.buttonStartStopTransmitting).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopTransmitting.getText().toString().equals("Start Transmitting")) {
                            transmitData(null); // send dummy data for tests
                            btnStartStopTransmitting.setText("Stop Transmitting!!!");
                        } else {
                            clientTransmiter.stopThread();
                            btnStartStopTransmitting.setText("Start Transmitting");
                        }
                    }
                });

        findViewById(R.id.buttonSendImage).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        Toast.makeText(myThis, "Choose image!!", Toast.LENGTH_SHORT).show();
                    }
                });


        findViewById(R.id.buttonStartStopServer).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopServer.getText().toString().equals("Start Receiving")) {
                            Context context = getApplicationContext();
                            String rcvPortNumber = ((EditText) findViewById(
                                    R.id.editTextServerPortNumber)).getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(
                                    ((EditText) findViewById(R.id.editTextMaxBufferSize)).getText().toString());

                            CharSequence text = "Start Receiving!!!!!";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();

                            if (isTcp)
                                clientReceiver = new ClientDataReceiverServerSocketThreadTCP(
                                        Integer.parseInt(rcvPortNumber)
                                        , ((EditText) findViewById(R.id.editTextRcvThrdRcvData))
                                        , ((EditText) findViewById(R.id.editTextRcvThrdSentData)), bufferSize);
                            else
                                clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                                        Integer.parseInt(rcvPortNumber)
                                        , ((EditText) findViewById(R.id.editTextRcvThrdRcvData)), bufferSize);
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
    }

    private void transmitData(Uri fileToSend) {
        Context context = getApplicationContext();
        String crIpAddress = ((EditText) findViewById(
                R.id.editTextCrIpAddress)).getText().toString();
        String crPortNumber = ((EditText) findViewById(
                R.id.editTextCrPortNumber)).getText().toString();
        String destIpAddress = ((EditText) findViewById(
                R.id.editTextDestIpAddress)).getText().toString();
        String destPortNumber = ((EditText) findViewById(
                R.id.editTextDestPortNumber)).getText().toString();
        String totalBytesToSend = ((EditText) findViewById(
                R.id.editTextTotalBytesToSend)).getText().toString();
        String delay = ((EditText) findViewById(R.id.editTextDelay)).getText().toString();
        int bufferSize = 1024 * Integer.parseInt(
                ((EditText) findViewById(R.id.editTextMaxBufferSize)).getText().toString());

        Toast.makeText(context, "Start transmitting!!!!!", Toast.LENGTH_SHORT).show();

        if (isTcp)
            clientTransmiter = new ClientSendDataThreadTCP(destIpAddress,
                    Integer.parseInt(destPortNumber)
                    , crIpAddress, Integer.parseInt(crPortNumber)
                    , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                    , ((EditText) findViewById(R.id.editTextTxThrdSentData))
                    , ((EditText) findViewById(R.id.editTextTxThrdRcvData))
                    , bufferSize, fileToSend);
        else
            clientTransmiter = new ClientSendDataThreadUDP(destIpAddress,
                    Integer.parseInt(destPortNumber)
                    , crIpAddress, Integer.parseInt(crPortNumber)
                    , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                    , ((EditText) findViewById(R.id.editTextTxThrdSentData))
                    , bufferSize); // todo add fileToSend

        clientTransmiter.start();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == RESULT_OK && null != data) {
            Uri uriFileToSend = data.getData();
            Log.d(WiFiDirectActivity.TAG,  "Start transmitting image: " + uriFileToSend.toString());
            Toast.makeText(this, "Start transmitting image: " + uriFileToSend.toString(), Toast.LENGTH_SHORT).show();
            transmitData(uriFileToSend); // send file
        }
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_SHORT).show();

        if (clientTransmiter != null)
            clientTransmiter.stopThread();

        if (clientReceiver != null)
            clientReceiver.stopThread();

        super.onDestroy();
    }

    public void onStop() {
        super.onStop();
        Toast.makeText(getApplicationContext(), "onStop", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(getApplicationContext(), "onPause", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Toast.makeText(getApplicationContext(), "onRestart", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(getApplicationContext(), "onResume", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(getApplicationContext(), "onStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(getApplicationContext(), "onBackPressed", Toast.LENGTH_SHORT).show();
    }


}
