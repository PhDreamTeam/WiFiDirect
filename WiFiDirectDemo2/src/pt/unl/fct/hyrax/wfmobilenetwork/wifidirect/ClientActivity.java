package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;

import java.io.IOException;
import java.net.*;
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

    public static String INITIAL_TCP_UDP_IPADDRESS = "192.168.49.1";
    public static int INITIAL_TCP_UDP_PORT = 3000;

    // MulticastSocket are limited in the range of 224.0.0.1 to 239.255.255.255.
    // Multicast ports between: 1025 and 49151
    public static String INITIAL_MULTICAST_UDP_IPADDRESS = "224.1.0.0";
    public static int INITIAL_MULTICAST_UDP_PORT = 10000;


    ClientActivity myThis;

    private Context context;
    private WifiManager wifiManager;

    IStoppable clientTransmitter;
    IStoppable clientReceiver;

    protected int CHOOSE_FILE_RESULT_CODE = 20;
    COMM_MODE communicationMode = COMM_MODE.TCP;


    private EditText editTextCrIpAddress;
    private EditText editTextCrPortNumber;
    private EditText editTextDestIpAddress;
    private EditText editTextDestPortNumber;
    private EditText editTextTotalBytesToSend;
    private EditText editTextDelay;
    private EditText editTextMaxBufferSize;
    private EditText etReceivePortNumber;

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
    private Button btnUdpMulticast;
    private LinearLayout llMulticastNetworkInterfaces;

    ArrayList<ReceptionGuiInfo> receptionGuiInfos = new ArrayList<>();
    private RadioButton rbMulticastNetIntNone;
    private RadioButton rbMulticastNetIntWFD;
    private RadioButton rbMulticastNetIntWF;

    private TextView tvWFDState;
    private TextView tvWFState;
    private NetworkInterface wfdNetworkInterface;
    private NetworkInterface wfNetworkInterface;
    private TextView tvCRAddress;
    private EditText etMulticastRcvIpAddress;
    private TextView tvReceptionAddress;
    private BroadcastReceiver broadcastReceiver;
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;

    NetworkInterfacesDetector networkInterfacesDetector;

    enum COMM_MODE {TCP, UDP, UDP_MULTICAST}


    /*
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(this, getMainLooper(), null);

        //AndroidUtils.toast(this, "onCreate");
        setContentView(R.layout.client_activity);

        // Interfaces state ==============================
        tvWFDState = (TextView) findViewById(R.id.tvCAWFDState);
        tvWFState = (TextView) findViewById(R.id.tvCAWFState);

        // Main zone =====================================
        btnTcp = (Button) findViewById(R.id.btnCATCP);
        btnUdp = (Button) findViewById(R.id.btnCAUDP);
        btnUdpMulticast = (Button) findViewById(R.id.btnCAUDPMulticast);

        btnStartTransmitting = (Button) findViewById(R.id.buttonCAStartTransmitting);
        btnStopTransmitting = (Button) findViewById(R.id.buttonCAStopTransmitting);
        btnStartServer = (Button) findViewById(R.id.buttonStartServer);
        btnStopServer = (Button) findViewById(R.id.buttonStopServer);
        btnSendImage = (Button) findViewById(R.id.buttonCASendImage);
        btnStopSendingImage = (Button) findViewById(R.id.buttonCAStopSendingImage);

        btnTdls = (Button) findViewById(R.id.buttonTdls);
        btnRegCrTdls = (Button) findViewById(R.id.buttonRegCrTdls);
        btnUnRegCrTdls = (Button) findViewById(R.id.buttonUnRegCrTdls);

        // Transmission zone =====================================
        tvTransmissionZone = findViewByIdAndCast(R.id.textViewTransmissionZone);
        llTransmissionZone = findViewByIdAndCast(R.id.LinearLayoutTransmission);
        llTransmissionInputZone = findViewByIdAndCast(R.id.linearLayoutTransmissionInputData);

        llMulticastNetworkInterfaces = findViewByIdAndCast(R.id.llCAUDPMulticastNetInterfaces);

        tvCRAddress = (TextView) findViewById(R.id.tvCACRAddress);
        editTextCrIpAddress = findViewByIdAndCast(R.id.editTextCrIpAddress);
        editTextCrPortNumber = findViewByIdAndCast(R.id.editTextCrPortNumber);
        editTextDestIpAddress = findViewByIdAndCast(R.id.editTextDestIpAddress);
        editTextDestPortNumber = findViewByIdAndCast(R.id.editTextDestPortNumber);
        editTextTotalBytesToSend = findViewByIdAndCast(R.id.editTextTotalBytesToSend);
        editTextDelay = findViewByIdAndCast(R.id.editTextDelay);
        editTextMaxBufferSize = findViewByIdAndCast(R.id.editTextMaxBufferSize);
        tvTxThrdSentData = findViewByIdAndCast(R.id.textViewCATxThrdSentData);
        tvTxThrdRcvData = findViewByIdAndCast(R.id.textViewCATxThrdRcvData);

        rbSentKBytes = (RadioButton) findViewById(R.id.radioButtonCAKBytesToSend);
        rbSentMBytes = (RadioButton) findViewById(R.id.radioButtonCAMBytesToSend);

        rbMulticastNetIntNone = (RadioButton) findViewById(R.id.rbCAUDPMulticastNetIntNone);
        rbMulticastNetIntWFD = (RadioButton) findViewById(R.id.rbCAUDPMulticastNetIntWFD);
        rbMulticastNetIntWF = (RadioButton) findViewById(R.id.rbCAUDPMulticastNetIntWF);


        // reception zone ========================================
        tvReceptionZone = findViewByIdAndCast(R.id.textViewReceptionZone);
        llReceptionZone = findViewByIdAndCast(R.id.LinearLayoutReception);
        llReceptionLogs = findViewByIdAndCast(R.id.linearLayoutReceptionLogs);
        btnClearReceptionLogs = (Button) findViewById(R.id.buttonCAClearReceptionLogs);

        rbReplyInfoNone = (RadioButton) findViewById(R.id.radioButtonClientReplyInfoNone);
        rbReplyInfoOKs = (RadioButton) findViewById(R.id.radioButtonClientReplyInfoOKs);
        rbReplyInfoEcho = (RadioButton) findViewById(R.id.radioButtonClientReplyInfoEcho);

        tvReceptionAddress = (TextView) findViewById(R.id.tvCAReceptionAddress);
        etMulticastRcvIpAddress = (EditText) findViewById(R.id.etCAMulticastRcvIpAddress);
        etReceivePortNumber = (EditText) findViewById(R.id.etCAReceivePortNumber);

        etMulticastRcvIpAddress.setText(INITIAL_MULTICAST_UDP_IPADDRESS);

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
                        communicationMode = COMM_MODE.UDP;
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
                        // change to UDP Multicast mode
                        communicationMode = COMM_MODE.UDP_MULTICAST;
                        btnUdp.setVisibility(View.GONE);
                        btnUdpMulticast.setVisibility(View.VISIBLE);
                        setEnabledRadioButtonsReplyMode(false);
                        btnSendImage.setEnabled(false);
                        llMulticastNetworkInterfaces.setVisibility(View.VISIBLE);
                        tvCRAddress.setText("MCR Address:");
                        editTextCrIpAddress.setText(INITIAL_MULTICAST_UDP_IPADDRESS);
                        editTextCrPortNumber.setText(String.valueOf(INITIAL_MULTICAST_UDP_PORT));
                        tvReceptionAddress.setText("Multicast Address: ");
                        etMulticastRcvIpAddress.setVisibility(View.VISIBLE);
                        etReceivePortNumber.setText(String.valueOf(INITIAL_MULTICAST_UDP_PORT));
                    }
                }
        );

        btnUdpMulticast.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // change to TCP mode
                        communicationMode = COMM_MODE.TCP;
                        btnUdpMulticast.setVisibility(View.GONE);
                        btnTcp.setVisibility(View.VISIBLE);
                        setEnabledRadioButtonsReplyMode(true);
                        btnSendImage.setEnabled(true);
                        llMulticastNetworkInterfaces.setVisibility(View.GONE);
                        tvCRAddress.setText("CR Address:");
                        editTextCrIpAddress.setText(INITIAL_TCP_UDP_IPADDRESS);
                        editTextCrPortNumber.setText(String.valueOf(INITIAL_TCP_UDP_PORT));
                        tvReceptionAddress.setText("Receive Port Number: ");
                        etMulticastRcvIpAddress.setVisibility(View.GONE);
                        etReceivePortNumber.setText(String.valueOf(INITIAL_TCP_UDP_PORT));
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


        // this is needed by Multicast sockets
        WifiManager wim = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wim != null) {
            WifiManager.MulticastLock mcLock = wim.createMulticastLock(TAG);
            mcLock.acquire();
        } else {
            throw new RuntimeException("Failed to get context.WIFI_SERVICE, to create Multicast lock");
        }

        // WFD info listener
        WifiP2pManager.GroupInfoListener wfdGroupInfoListener = new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group == null) {
                    tvWFDState.setText("WFD:  OFF/NC");
                    wfdNetworkInterface = null;
                    rbMulticastNetIntWFD.setVisibility(View.GONE);
                    return;
                }

                // group available: device connected

                // update global vars and GUI
                wfdNetworkInterface = AndroidUtils.getNetworkInterface(group.getInterface());
                rbMulticastNetIntWFD.setVisibility(View.VISIBLE);

                // update GUI with info message
                tvWFDState.setText("WFD:  (" + (group.isGroupOwner() ? "GO" : group.getOwner().deviceName) +
                        ")  " + group.getInterface() + "  " +
                        WiFiDirectControlActivity.getLocalIpAddress(group.getInterface()));
            }
        };

        // FD info listener
        NetworkInterfacesDetector.WFNetworkInterfaceListener wfNetworkInterfaceListener = new NetworkInterfacesDetector.WFNetworkInterfaceListener() {
            public void updateWFNetworkInterface(NetworkInterface wfInterface) {
                wfNetworkInterface = wfInterface;

                if (wfInterface == null) {
                    tvWFState.setText("WF:  OFF/NC");
                    rbMulticastNetIntWF.setVisibility(View.GONE);
                } else {
                    tvWFState.setText("WF:  " + AndroidUtils.getWifiSSID(ClientActivity.this) + "  " +
                            wfInterface.getName() + "  " +
                            AndroidUtils.getWifiLinkSpeed(ClientActivity.this) + "Mbps  " +
                            getIPV4AddressWithBroadcast(wfInterface).toString().substring(1));
                    rbMulticastNetIntWF.setVisibility(View.VISIBLE);
                }
            }
        };

        // create network interfaces detector
        networkInterfacesDetector = new NetworkInterfacesDetector(this, wfdGroupInfoListener,
                wfNetworkInterfaceListener);

        //        updateNetworkInterfaces();
        networkInterfacesDetector.updateNetworkInterfaces();
    }


    /*
     *
     */
    public static InetAddress getIPV4AddressWithBroadcast(NetworkInterface netInterface) {
        for (InterfaceAddress intAddr : netInterface.getInterfaceAddresses()) {
            if ((intAddr.getAddress() instanceof Inet4Address) && intAddr.getBroadcast() != null)
                return intAddr.getAddress();
        }
        return null;
    }


    /**
     *
     */
    public void startReceivingGuiActions() {
        setEnabledRadioButtonsReplyMode(false);
        etReceivePortNumber.setEnabled(false);
        btnStartServer.setVisibility(View.GONE);
        btnStopServer.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    public void endReceivingGuiActions() {
        btnStopServer.setVisibility(View.GONE);
        btnStartServer.setVisibility(View.VISIBLE);
        etReceivePortNumber.setEnabled(true);
        if (communicationMode == COMM_MODE.TCP)
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
            if (communicationMode == COMM_MODE.TCP)
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
        String rcvPortNumber = etReceivePortNumber.getText().toString();
        int bufferSize = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

        try {
            switch (communicationMode) {
                case TCP:
                    clientReceiver = new ClientDataReceiverServerSocketThreadTCP(
                            Integer.parseInt(rcvPortNumber), llReceptionLogs, bufferSize, getReplyMode(), this);
                    break;

                case UDP:
                    // build the UDP receiver socket
                    UDPSocket sock = UDPSocket.getUDPReceiverSocket(Integer.parseInt(rcvPortNumber));

                    // create worker thread receiving in UDP socket
                    clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                            sock, llReceptionLogs, bufferSize, this);
                    break;

                case UDP_MULTICAST:
                    // get interface if any
                    NetworkInterface netInterface = rbMulticastNetIntWFD.isChecked() ? wfdNetworkInterface :
                            rbMulticastNetIntWF.isChecked() ? wfNetworkInterface : null;

                    String multicastIpAddress = etMulticastRcvIpAddress.getText().toString();

                    // build the UDP receiver socket
                    UDPSocket msock = UDPSocket.getUDPMulticastReceiverSocket(
                            multicastIpAddress, Integer.parseInt(rcvPortNumber), netInterface);

                    // create worker thread receiving in UDP socket
                    clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                            msock, llReceptionLogs, bufferSize, this);
            }


            clientReceiver.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        try {
            switch (communicationMode) {
                case TCP:
                    clientTransmitter = new ClientSendDataThreadTCP(destIpAddress,
                            destPortNumber
                            , crIpAddress, crPortNumber
                            , delayMs, totalBytesToSend
                            , tvTxThrdSentData
                            , tvTxThrdRcvData
                            , this
                            , bufferSizeBytes, fileToSend);
                    break;

                case UDP:
                    // build the UDP transmitter socket
                    UDPSocket sock = UDPSocket.getUDPTransmitterSocket(crIpAddress, crPortNumber);

                    // create worker thread transmitting by UDP socket
                    clientTransmitter = new ClientSendDataThreadUDP(destIpAddress, destPortNumber
                            , sock, delayMs, totalBytesToSend, tvTxThrdSentData, this, bufferSizeBytes);
                    break;

                case UDP_MULTICAST:
                    // get interface if any
                    NetworkInterface netInterface = rbMulticastNetIntWFD.isChecked() ? wfdNetworkInterface :
                            rbMulticastNetIntWF.isChecked() ? wfNetworkInterface : null;

                    Log.d(TAG, "Multicast transmitter: will use network interface: " + netInterface);

                    // build he UDP multicast transmitter socket
                    UDPSocket msock = UDPSocket.getUDPMulticastTransmitterSocket(crIpAddress, crPortNumber,
                            netInterface);

                    // create worker thread transmitting in UDP multicast socket
                    clientTransmitter = new ClientSendDataThreadUDP(destIpAddress, destPortNumber
                            , msock, delayMs, totalBytesToSend, tvTxThrdSentData, this, bufferSizeBytes);

            }

            // start worker thread
            clientTransmitter.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

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
        networkInterfacesDetector.onPause();
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
        networkInterfacesDetector.onResume();
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
        // AndroidUtils.toast(this, "onBackPressed");
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

/*
 *
 */
class UDPSocket {
    String crIpAddress;
    int crPort;

    DatagramSocket socket;

    /*
     *
     */
    public UDPSocket(int crPort, DatagramSocket socket) {
        this(null, crPort, socket);
    }

    /*
     *
     */
    public UDPSocket(String crIpAddress, int crPort, DatagramSocket socket) {
        this.crIpAddress = crIpAddress;
        this.crPort = crPort;
        this.socket = socket;
    }

    /*
     *
     */
    public String getCrIpAddress() {
        return crIpAddress;
    }

    /*
     *
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /*
         *
         */
    public int getCrPort() {
        return crPort;
    }

    /*
         * get UDP Transmitter Socket
         */
    public static UDPSocket getUDPTransmitterSocket(String crIpAddress, int crPort) throws UnknownHostException, SocketException {
        DatagramSocket socket = new DatagramSocket();
        return new UDPSocket(crIpAddress, crPort, socket);
    }

    /*
     * get UDP Multicast Transmitter Socket
     */
    public static UDPSocket getUDPMulticastTransmitterSocket(String crMulticastIpAddress, int crMulticastPort,
                                                             NetworkInterface netInterface) throws IOException {
        // get multicast socket, in a dynamic port
        MulticastSocket txSocket = new MulticastSocket();
        txSocket.setTimeToLive(20);

        if (netInterface != null)
            txSocket.setNetworkInterface(netInterface);

        return new UDPSocket(crMulticastIpAddress, crMulticastPort, txSocket);
    }

    /*
     * get UDP Receiver Socket
     */
    public static UDPSocket getUDPReceiverSocket(int receivingPort) throws UnknownHostException, SocketException {
        DatagramSocket serverSocket = new DatagramSocket(receivingPort);
        return new UDPSocket(receivingPort, serverSocket);
    }


    /*
         * get UDP Multicast Receiver Socket
         */
    public static UDPSocket getUDPMulticastReceiverSocket(String crMulticastIpAddress, int crMulticastPort,
                                                          NetworkInterface netInterface) throws IOException {
        // get multicast socket in a local port - must be the same
        MulticastSocket multicastReceiverSocket = new MulticastSocket(crMulticastPort);

        // create multicast socket endpoint
        InetSocketAddress iSock = new InetSocketAddress(crMulticastIpAddress, crMulticastPort);

        //Log.d(TAG, "Multicast receiver: will use network interface: " + netInterface);

        // joint the sockets to the multicast group defined by the multicast endpoint using the
        // specified interface
        multicastReceiverSocket.joinGroup(iSock, netInterface);

        // return the UDPSocket wrapper class
        return new UDPSocket(crMulticastIpAddress, crMulticastPort, multicastReceiverSocket);
    }

}

