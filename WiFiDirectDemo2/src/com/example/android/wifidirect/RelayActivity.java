package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Created by DR & AT on 20/05/2015.
 *
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
        btnTcpUdp = (Button) findViewById(R.id.buttonTcpUdp);

        isTcp = btnTcpUdp.getText().toString().equals("TCP");

        printNetworkInfo(getApplicationContext());

        findViewById(R.id.buttonStartStop).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(btnStartStop.getText().toString().equals("Start Relaying")) {
                            Context context = getApplicationContext();
                            String CRPort = ((EditText) findViewById(R.id.editTextCrPortNumber)).getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(((EditText) findViewById(R.id.editTextCRMaxBufferSize)).getText().toString());

                            CharSequence text = "Start Relaying at port: "+ CRPort +"!!!!!";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();

                            if (isTcp)
                                crForwarder = new CrForwardServerTCP(Integer.parseInt(CRPort)
                                        , ((TextView) findViewById(R.id.textViewTransferedDataOrigDest))
                                        , ((TextView) findViewById(R.id.textViewTransferedDataDestOrig))
                                        , bufferSize);
                            else
                                crForwarder = new CrForwardServerUDP(Integer.parseInt(CRPort)
                                        , ((TextView) findViewById(R.id.textViewTransferedDataOrigDest))
                                        , bufferSize);

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

    @Override
    protected void onDestroy() {
        Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_SHORT).show();

        if(crForwarder != null)
            crForwarder.stopThread();

        super.onDestroy();
    }

    private void printNetworkInfo(Context context) {
        // Debug networks
        String netStr = "Networks2: ";

//    TESTE 1
        ConnectivityManager connMng = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nia[] = connMng.getAllNetworkInfo();
        for (NetworkInfo ni : nia) {
            netStr += ni.getTypeName() + ", " + ni.getExtraInfo() + "\n";
        }
        ((TextView) findViewById(R.id.textViewNetInfo)).append("getAllNetworkInfo: \n" + netStr);

       Toast toast2 = Toast.makeText(context, netStr, Toast.LENGTH_SHORT);
       toast2.show();

//    TESTE 2 - listar o nome das interfaces de rede
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                netStr += netint.getName() + ", " + netint.getDisplayName() + "\n";
            }
            ((TextView) findViewById(R.id.textViewNetInfo)).append("getNetworkInterfaces: \n" + netStr);
            Toast toast3 = Toast.makeText(context, netStr, Toast.LENGTH_SHORT);
            toast3.show();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }
}