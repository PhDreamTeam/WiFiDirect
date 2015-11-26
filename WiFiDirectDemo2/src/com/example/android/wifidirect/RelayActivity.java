package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Created by DR & AT on 20/05/2015
 * .
 */
public class RelayActivity extends Activity {
    RelayActivity myThis;
    IStoppable crForwarder;
    Button btnStartStop, btnTcpUdp;
    boolean isTcp;
    private Button btnNewRule;
    private EditText etCRNewRuleTo;
    private EditText etCRNewRuleUse;
    private TableLayout tableLayoutCRRules;
    private HashMap<String,String> relayRulesMap = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relay_activity);
        btnStartStop = (Button) findViewById(R.id.buttonStartStop);
        btnTcpUdp = (Button) findViewById(R.id.buttonRATcpUdp);

        etCRNewRuleTo = (EditText) findViewById(R.id.editTextCRNewRuleTo);
        etCRNewRuleUse = (EditText) findViewById(R.id.editTextCRNewRuleUse);

        btnNewRule = (Button) findViewById(R.id.buttonCRAddNewCRRule);
        tableLayoutCRRules = (TableLayout) findViewById(R.id.tableLayoutRelayRules);

        isTcp = btnTcpUdp.getText().toString().equals("TCP");

        printNetworkInfo(getApplicationContext());

        btnStartStop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnStartStop.getText().toString().equals("Start Relaying")) {
                            Context context = getApplicationContext();
                            String CRPort = ((EditText) findViewById(R.id.editTextCrPortNumber)).getText().toString();
                            int bufferSize = 1024 * Integer.parseInt(
                                    ((EditText) findViewById(R.id.editTextCRMaxBufferSize)).getText().toString());

                            CharSequence text = "Start Relaying at localPort: " + CRPort + "!!!!!";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                            toast.show();

                            if (isTcp)
                                crForwarder = new CrForwardServerTCP(Integer.parseInt(CRPort)
                                        , ((TextView) findViewById(R.id.textViewTransferedDataOrigDest))
                                        , ((TextView) findViewById(R.id.textViewTransferedDataDestOrig))
                                        , bufferSize, relayRulesMap);
                            else
                                crForwarder = new CrForwardServerUDP(Integer.parseInt(CRPort)
                                        , ((TextView) findViewById(R.id.textViewTransferedDataOrigDest))
                                        , bufferSize);

                            crForwarder.start();
                            btnStartStop.setText("Stop Relaying!!!");
                        } else {
                            crForwarder.stopThread();
                            btnStartStop.setText("Start Relaying");
                        }
                    }
                });

        btnTcpUdp.setOnClickListener(
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

        btnNewRule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewCRRule(etCRNewRuleTo.getText().toString(), etCRNewRuleUse.getText().toString());
            }
        });

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
     *
     */
    private void addNewCRRule(String toAddress, String useCRAddress) {
        TableRow tr = new TableRow(this);

        TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT
                , TableRow.LayoutParams.WRAP_CONTENT, 1.0f);

        TextView tvTo = new TextView(this);
        tvTo.setGravity(Gravity.CENTER);
        tvTo.setLayoutParams(trp);
        tvTo.setText(toAddress);
        tr.addView(tvTo);

        TextView tvUse = new TextView(this);
        tvUse.setGravity(Gravity.CENTER);
        tvUse.setLayoutParams(trp);
        tvUse.setText(useCRAddress);
        tr.addView(tvUse);

        tableLayoutCRRules.addView(tr);

        // Save rule
        relayRulesMap.put(toAddress, useCRAddress);
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_SHORT).show();

        if (crForwarder != null)
            crForwarder.stopThread();

        super.onDestroy();
    }

    private void printNetworkInfo(Context context) {
        // Debug networks
        String netStr = "";

//    TESTE 1
        ConnectivityManager connMng = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nia[] = connMng.getAllNetworkInfo();
        for (NetworkInfo ni : nia) {
            netStr += "\t" + ni.getTypeName() + ", " + ni.getType() + "\n";
        }
        ((TextView) findViewById(R.id.textViewNetInfo)).append("getAllNetworkInfo: \n" + netStr);

//       Toast toast2 = Toast.makeText(context, netStr, Toast.LENGTH_SHORT);
//       toast2.show();

//    TESTE 2 - listar o nome das interfaces de rede
        Enumeration<NetworkInterface> nets = null;
        netStr = "";
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                netStr += "\t" + netint.getName() + ", " + netint.getDisplayName() + "\n";
                Enumeration<InetAddress> inetAdds = netint.getInetAddresses();
                for (InetAddress inetAddr : Collections.list((inetAdds))) {
                    netStr += "\t\t" + inetAddr + "\n";
                }
            }
            ((TextView) findViewById(R.id.textViewNetInfo)).append("\nGetNetworkInterfaces: \n" + netStr);

//            Toast toast3 = Toast.makeText(context, netStr, Toast.LENGTH_SHORT);
//            toast3.show();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        //    TESTE 3 - listar o nome das interfaces de rede - API 21
//        Network[] nets2 = null;
//        netStr = "";
//        nets2 = connMng.getAllNetworks();
//        for (Network netint : nets2) {
//            netStr += "\t" + netint.toString() + "\n";
//        }
//        ((TextView) findViewById(R.id.textViewNetInfo)).append("\nGetAllNetworks: \n" + netStr);

//            Toast toast3 = Toast.makeText(context, netStr, Toast.LENGTH_SHORT);
//            toast3.show();

//    TESTE 4 - getActiveNetworkInfo - API 21
//        NetworkInfo netInfo = connMng.getActiveNetworkInfo();
//        ((TextView) findViewById(R.id.textViewNetInfo)).append("\nGetActiveNetworkInfo: \n" + netInfo.toString());
//
//        Network net = ConnectivityManager.getProcessDefaultNetwork();
//        ((TextView) findViewById(R.id.textViewNetInfo)).append(
//                "\nGetProcessDefaultNetwork: " + (net == null ? "null" : net.toString()));
//
//        //    TESTE 5 - getActiveNetworkInfo
//
//        // get WIFI Network
//        NetworkRequest netReq = new NetworkRequest.Builder().
//                addTransportType(NetworkCapabilities.TRANSPORT_WIFI).
//                build();
//
//        ConnectivityManager.NetworkCallback netCallBack = new ConnectivityManager.NetworkCallback() {
//            public void onAvailable(Network network) {
//                ((TextView) findViewById(R.id.textViewNetInfo)).append(
//                        "\nNetCallBack fired available: " + network);
//            }
//
//            public void onLost(Network network) {
//                ((TextView) findViewById(R.id.textViewNetInfo)).append(
//                        "\nNetCallBack lost fired: " + network);
//            }
//        };
//
//        connMng.requestNetwork(netReq, netCallBack);
//
//        // get P2P Network
//        // cada vez que se activa este callback d� um erro "o Wi-Fi Direct foi imterrompido"
//        // mesmo que comentado o interior do callback
//        // CONCLUS�O: N�O SE PODE UTILIZAR NO "OPO"
//        NetworkRequest netReq2 = new NetworkRequest.Builder().
//                addCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P).
//                //addTransportType(NetworkCapabilities.TRANSPORT_WIFI).
//                build();
//
//        ConnectivityManager.NetworkCallback netCallBack2 = new ConnectivityManager.NetworkCallback() {
//            public void onAvailable(Network network) {
//                ((TextView) findViewById(R.id.textViewNetInfo)).append(
//                        "\nNetCallBackP2P fired available: " + network);
//            }
//
//            public void onLost(Network network) {
//                ((TextView) findViewById(R.id.textViewNetInfo)).append(
//                        "\nNetCallBackP2P lost fired: " + network);
//            }
//        };
//        connMng.requestNetwork(netReq2, netCallBack2);

    }
}