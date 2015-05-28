package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);
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
                            Context context = getApplicationContext();
                            String crIpAddress = ((EditText) findViewById(R.id.editTextCrIpAddress)).getText().toString();
                            String crPortNumber = ((EditText) findViewById(R.id.editTextCrPortNumber)).getText().toString();
                            String destIpAddress = ((EditText) findViewById(R.id.editTextDestIpAddress)).getText().toString();
                            String destPortNumber = ((EditText) findViewById(R.id.editTextDestPortNumber)).getText().toString();
                            String totalBytesToSend = ((EditText) findViewById(R.id.editTextTotalBytesToSend)).getText().toString();
                            String delay = ((EditText) findViewById(R.id.editTextDelay)).getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(((EditText) findViewById(R.id.editTextMaxBufferSize)).getText().toString());

                            CharSequence text = "Start transmitting!!!!!";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();

                            if (isTcp)
                                clientTransmiter = new ClientSendDataThreadTCP(destIpAddress, Integer.parseInt(destPortNumber)
                                        , crIpAddress, Integer.parseInt(crPortNumber)
                                        , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                                        , ((EditText) findViewById(R.id.editTextSentData))
                                        , bufferSize);
                            else
                                clientTransmiter = new ClientSendDataThreadUDP(destIpAddress, Integer.parseInt(destPortNumber)
                                        , crIpAddress, Integer.parseInt(crPortNumber)
                                        , Long.parseLong(delay), Long.parseLong(totalBytesToSend)
                                        , ((EditText) findViewById(R.id.editTextSentData))
                                        , bufferSize);

                            clientTransmiter.start();
                            btnStartStopTransmitting.setText("Stop Transmitting!!!");
                        } else {
                            clientTransmiter.stopThread();
                            btnStartStopTransmitting.setText("Start Transmitting");
                        }
                    }
                });


        findViewById(R.id.buttonStartStopServer).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStopServer.getText().toString().equals("Start Receiving")) {
                            Context context = getApplicationContext();
                            String rcvPortNumber = ((EditText) findViewById(R.id.editTextServerPortNumber)).getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(((EditText) findViewById(R.id.editTextMaxBufferSize)).getText().toString());

                            CharSequence text = "Start Receiving!!!!!";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();

                            if (isTcp)
                                clientReceiver = new ClientDataReceiverServerSocketThreadTCP(Integer.parseInt(rcvPortNumber)
                                        , ((EditText) findViewById(R.id.editTextRcvData)), bufferSize);
                            else
                                clientReceiver = new ClientDataReceiverServerSocketThreadUDP(Integer.parseInt(rcvPortNumber)
                                        , ((EditText) findViewById(R.id.editTextRcvData)), bufferSize);
                            clientReceiver.start();

                            btnStartStopServer.setText("Stop Receiving!!!");
                        } else {
                            clientReceiver.stopThread();
                            btnStartStopServer.setText("Start Receiving");
                        }
                    }
                });

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
                });
    }

}
