package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by DR & AT on 20/05/2015.
 */
public class RelayActivity extends Activity {
    RelayActivity myThis;
    IStopable crForwarder;
    Button btnStartStop, btnTcpUdp;
    boolean isTcp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relay_activity);
        btnStartStop = (Button) findViewById(R.id.buttonStartStop);

        isTcp = btnTcpUdp.getText().toString().equals("TCP");


        findViewById(R.id.buttonStartStop).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(btnStartStop.getText().toString().equals("Start Relaying")) {
                            Context context = getApplicationContext();
                            String CRPort = ((EditText) findViewById(R.id.editTextCrPortNumber)).getText().toString();

                            CharSequence text = "Start Relaying at port: "+ CRPort +"!!!!!";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();

                            if (isTcp)
                                crForwarder = new CrForwardServerTCP(Integer.parseInt(CRPort), ((TextView) findViewById(R.id.textViewTransferedData)));
                            //TODO UDP
//                            else
//                                crForwarder = new CrForwardServerUDP(Integer.parseInt(CRPort), ((TextView) findViewById(R.id.textViewTransferedData)));

                            crForwarder.start();
                            btnStartStop.setText("Stop Relaying!!!");
                        }else{
                            crForwarder.stopThread();
                            btnStartStop.setText("Start Relaying");
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