package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.*;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.Configurations;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.SystemInfo;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

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
    public static int INITIAL_TCP_UDP_PORT = 30000;

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
    private EditText editTextCrIpAddressLastPart;
    private EditText editTextCrPortNumber;
    private EditText editTextDestIpAddress;
    private EditText editTextDestPortNumber;
    private EditText editTextTotalBytesToSend;
    private EditText editTextDelay;
    private EditText editTextMaxBufferSize;
    private EditText etReceivePortNumber;

    private Button btnStartServer, btnSendData, btnTcp;
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
    private int numberOfTransmittingTestsToDo;
    private int numberOfCurrentTest = 1;
    private Timer transmittingTimer;
    private int delayBeforeEachTransmitTestMS;
    private Runnable transmitTask;
    private Handler transmitHandler;
    private AlertDialog alertDialogScreenOff;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenOffBroadcastReceiver;
    private NotificationManager notificationManager;
    private Uri notificationSoundUri;
    private Uri notificationSoundEndUri;
    private boolean isWakeLockRunning;

    IntentFilter intentFilterScreenOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    private WifiP2pGroup p2pLastKnownGroup;
    private Configurations configurations;
    private EditText etLogDir;
    private Button btnChangeLogDir;

    private String multicastLogDirIpAddress = "227.1.0.0";
    private int multicastLogDirPort = 10000;
    private Button btnUpdateLogDirReceiver;

    private LogDirMulticastSocketContainer logDirMulticastDataWFD;
    private LogDirMulticastSocketContainer logDirMulticastDataWF;
    private Button btnSentKBytes;
    private Button btnSentMBytes;
    private LinearLayout llReceptionReplyMode;
    private Button btnBindToNetwork;
    private Network networkWifi;
    private Button btnActivateLogDirSystem;
    private Button btnDeactivateLogDirSystem;
    private WifiManager.WifiLock wifiLock;
    private WifiManager.MulticastLock wifiMulticastLock;
    private ConnectivityManager conManager;
    private Network networkWifiDirect;
    private BIND_TO_NETWORK currentBindToNetwork = BIND_TO_NETWORK.NONE;
    private Button btnSendStartTest;

    private String lastStartTestMsg = "";
    private int nextStartTestID = 1;
    private Button btnCancelStartTest;
    private Button btnEditStartTest;
    private Handler guiThreadLoopHandler;
    private String deviceName;

    enum COMM_MODE {TCP, UDP, UDP_MULTICAST}

    enum BIND_TO_NETWORK {NONE, WF, WFD}

    /**
     *
     */
    class LogDirMulticastSocketContainer {
        boolean isWFDInterface;
        MulticastSocket multicastSocket;
        Thread thread;

        /**
         *
         */
        public LogDirMulticastSocketContainer(boolean isWFDInterface) {
            this.isWFDInterface = isWFDInterface;
        }

        /**
         *
         */
        public NetworkInterface getNetworkInterface() {
            return isWFDInterface ? wfdNetworkInterface : wfNetworkInterface;
        }

        /**
         *
         */
        void stopReceiving() {
            if (thread != null) {
                Log.d(TAG, "Stop receiving logDir system, interface: " + (isWFDInterface ? "WFD" : "WF"));
                // signal thread to end
                thread.interrupt();
                // close socket to force wakeup thread
                multicastSocket.close();
            }
        }
    }

    /**
     *
     */
    public Network getNetworkWF() {
        return networkWifi;
    }

    /**
     *
     */
    public Network getNetworkWFD() {
        return networkWifiDirect;
    }


    /**
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        myThis = this;
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        p2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        channel = p2pManager.initialize(this, getMainLooper(), null);

        guiThreadLoopHandler = new Handler();

        // needed for screen off tests
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WFDScreenOFF");
        wakeLock.acquire();
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "wifiLock");
        wifiLock.acquire();
        // multicast lock created, but only activated only when multicast is selected
        // I suspect that this slows down communications
        wifiMulticastLock = wifiManager.createMulticastLock("wifiMulticastLock");

        notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationSoundEndUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        configurations = Configurations.readFromConfigurationsFile();

        deviceName = configurations.getDeviceName();

        lastStartTestMsg = "start " + deviceName + " ";

        //AndroidUtils.toast(this, "onCreate");
        setContentView(R.layout.client_activity);

        // Interfaces state ==============================
        tvWFDState = (TextView) findViewById(R.id.tvCAWFDState);
        tvWFState = (TextView) findViewById(R.id.tvCAWFState);

        // Log Dir ========================================
        etLogDir = (EditText) findViewById(R.id.etCALogDir);
        btnChangeLogDir = (Button) findViewById(R.id.btnCAChangeLogDir);
        btnUpdateLogDirReceiver = (Button) findViewById(R.id.btnCAUpdateLogDirReceiver);
        btnActivateLogDirSystem = (Button) findViewById(R.id.btnCAActivateLogDirSystem);
        btnDeactivateLogDirSystem = (Button) findViewById(R.id.btnCADeactivateLogDirSystem);
        btnEditStartTest = (Button) findViewById(R.id.btnCAEditStartTest);
        btnSendStartTest = (Button) findViewById(R.id.btnCASendStartTest);
        btnCancelStartTest = (Button) findViewById(R.id.btnCACancelStartTest);

        // Main zone =====================================
        btnTcp = (Button) findViewById(R.id.btnCATCP);
        btnUdp = (Button) findViewById(R.id.btnCAUDP);
        btnUdpMulticast = (Button) findViewById(R.id.btnCAUDPMulticast);

        btnSendData = (Button) findViewById(R.id.btnCASendData);
        btnStopTransmitting = (Button) findViewById(R.id.btnCAStopTransmitting);
        btnStartServer = (Button) findViewById(R.id.btnStartServer);
        btnStopServer = (Button) findViewById(R.id.btnStopServer);
        btnSendImage = (Button) findViewById(R.id.btnCASendImage);
        btnStopSendingImage = (Button) findViewById(R.id.btnCAStopSendingImage);

        btnTdls = (Button) findViewById(R.id.buttonTdls);
        btnRegCrTdls = (Button) findViewById(R.id.buttonRegCrTdls);
        btnUnRegCrTdls = (Button) findViewById(R.id.buttonUnRegCrTdls);

        // Transmission zone =====================================
        tvTransmissionZone = findViewByIdAndCast(R.id.textViewTransmissionZone);
        llTransmissionZone = findViewByIdAndCast(R.id.LinearLayoutTransmission);
        llTransmissionInputZone = findViewByIdAndCast(R.id.linearLayoutTransmissionInputData);

        llMulticastNetworkInterfaces = findViewByIdAndCast(R.id.llCAUDPMulticastNetInterfaces);

        tvCRAddress = (TextView) findViewById(R.id.tvCACRAddress);
        editTextCrIpAddress = (EditText) findViewById(R.id.editTextCrIpAddress);
        editTextCrIpAddressLastPart = (EditText) findViewById(R.id.editTextCrIpAddressLastPart);
        editTextCrPortNumber = (EditText) findViewById(R.id.editTextCrPortNumber);
        editTextDestIpAddress = (EditText) findViewById(R.id.editTextDestIpAddress);
        editTextDestPortNumber = (EditText) findViewById(R.id.editTextDestPortNumber);
        editTextTotalBytesToSend = (EditText) findViewById(R.id.editTextTotalBytesToSend);
        editTextDelay = (EditText) findViewById(R.id.editTextDelay);
        editTextMaxBufferSize = (EditText) findViewById(R.id.editTextMaxBufferSize);
        tvTxThrdSentData = (TextView) findViewById(R.id.textViewCATxThrdSentData);
        tvTxThrdRcvData = (TextView) findViewById(R.id.textViewCATxThrdRcvData);

        btnSentKBytes = (Button) findViewById(R.id.btnCAKBytesToSend);
        btnSentMBytes = (Button) findViewById(R.id.btnCAMBytesToSend);

        rbMulticastNetIntNone = (RadioButton) findViewById(R.id.rbCAUDPMulticastNetIntNone);
        rbMulticastNetIntWFD = (RadioButton) findViewById(R.id.rbCAUDPMulticastNetIntWFD);
        rbMulticastNetIntWF = (RadioButton) findViewById(R.id.rbCAUDPMulticastNetIntWF);

        btnBindToNetwork = (Button) findViewById(R.id.btnCABindToNetwork);

        // reception zone ========================================
        tvReceptionZone = findViewByIdAndCast(R.id.textViewReceptionZone);
        llReceptionZone = findViewByIdAndCast(R.id.LinearLayoutReception);
        llReceptionLogs = findViewByIdAndCast(R.id.linearLayoutReceptionLogs);
        btnClearReceptionLogs = (Button) findViewById(R.id.buttonCAClearReceptionLogs);

        llReceptionReplyMode = (LinearLayout) findViewById(R.id.linearLayoutCAReceptionReplyMode);

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
        etLogDir.setText(logDir);
        etLogDir.setEnabled(false);


        // set listeners on buttons

        btnActivateLogDirSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateLogDirSystem();
            }
        });

        btnDeactivateLogDirSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deactivateLogDirSystem();
            }
        });

        btnChangeLogDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etLogDir.isEnabled()) {
                    etLogDir.setEnabled(false);
                    processNewLogDir(etLogDir.getText().toString(), null);
                    // avoid keyboard popping up
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    etLogDir.setBackgroundColor(0xff800000);
                } else {
                    // keyboard pop up
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    // enabled input
                    etLogDir.setEnabled(true);
                    // request focus
                    etLogDir.requestFocus();
                    // to put focus on the right
                    etLogDir.setSelection(etLogDir.getText().toString().length());
                    etLogDir.setBackgroundColor(0xff500000);
                }
            }
        });
        etLogDir.setBackgroundColor(0xff800000);

        btnUpdateLogDirReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLogDirReceiver();
            }
        });

        btnEditStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnEditStartTest.setVisibility(View.GONE);
                btnDeactivateLogDirSystem.setVisibility(View.GONE);
                btnChangeLogDir.setVisibility(View.GONE);
                btnUpdateLogDirReceiver.setVisibility(View.GONE);

                btnSendStartTest.setVisibility(View.VISIBLE);
                btnCancelStartTest.setVisibility(View.VISIBLE);

                // keyboard pop up
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                // enabled input
                etLogDir.setEnabled(true);
                // request focus
                etLogDir.requestFocus();

                // put last startTestMsg but without the last space and number sequence
                etLogDir.setText(lastStartTestMsg.substring(0, lastStartTestMsg.lastIndexOf(" ")).trim() + ' ');

                // to put focus on the right
                etLogDir.setSelection(etLogDir.getText().toString().length());
                etLogDir.setBackgroundColor(0xff500000);
            }
        });


        btnSendStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSendStartTest_OnClickListener();
            }
        });

        btnCancelStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSendStartTest.setVisibility(View.GONE);
                btnCancelStartTest.setVisibility(View.GONE);
                btnDeactivateLogDirSystem.setVisibility(View.VISIBLE);
                btnChangeLogDir.setVisibility(View.VISIBLE);
                btnEditStartTest.setVisibility(View.VISIBLE);
                btnUpdateLogDirReceiver.setVisibility(View.VISIBLE);

                etLogDir.setText(logDir);
                etLogDir.setEnabled(false);
                processNewLogDir(etLogDir.getText().toString(), null);
                // avoid keyboard popping up
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                etLogDir.setBackgroundColor(0xff800000);
            }
        });


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

        btnSentKBytes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSentKBytes.setVisibility(View.GONE);
                btnSentMBytes.setVisibility(View.VISIBLE);
            }
        });

        btnSentMBytes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSentMBytes.setVisibility(View.GONE);
                btnSentKBytes.setVisibility(View.VISIBLE);
            }
        });

        btnSendData.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // start transmitting
                        startTransmittingGuiActions(false);
                        doTransmit(null);   // send dummy data for tests

                        numberOfTransmittingTestsToDo = numberOfCurrentTest = 1;
                    }
                });

        btnStopTransmitting.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // stop transmitting
                        clientTransmitter.stopThread();
                        endTransmittingGuiActions(null);
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

        btnBindToNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = btnBindToNetwork.getText().toString();
                if (text.equalsIgnoreCase("No bind")) {
                    btnBindToNetwork.setText("Bind to WF");
                    currentBindToNetwork = BIND_TO_NETWORK.WF;
                    // get network wifi to be ready to be used with "bind to network"
                    requestNetworkWF();

                } else if (text.equalsIgnoreCase("Bind to WF")) {
                    btnBindToNetwork.setText("Bind to WFD");
                    currentBindToNetwork = BIND_TO_NETWORK.WFD;
                    // get network wifiDirect to be ready to be used with "bind to network"
                    requestNetworkWFD();

                } else if (text.equalsIgnoreCase("Bind to WFD")) {
                    btnBindToNetwork.setText("No bind");
                    currentBindToNetwork = BIND_TO_NETWORK.NONE;
                } else throw new IllegalStateException("btnBindToNetwork with invalid value: " + text);
            }
        });

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
                        stopReceivingServerActions();
                    }
                }
        );

        btnClearReceptionLogs.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearReceptionLogs();
                    }
                }
        );

        btnTcp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setUdpCommunicationMode();
                    }
                }
        );

        btnUdp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setUdpMulticastCommunicationMode();
                    }
                }
        );

        btnUdpMulticast.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTcpCommunicationMode();
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
                        String crIpAddress = editTextCrIpAddress.getText().toString() + "." +
                                editTextCrIpAddressLastPart.getText().toString();
                        setTdlsEnabled(crIpAddress, true);
                    }
                }
        );

        btnUnRegCrTdls.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String crIpAddress = editTextCrIpAddress.getText().toString() + "." +
                                editTextCrIpAddressLastPart.getText().toString();
                        setTdlsEnabled(crIpAddress, false);
                    }
                }
        );

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // WFD info listener
        WifiP2pManager.GroupInfoListener wfdGroupInfoListener = new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group == null) {
                    tvWFDState.setText("WFD:  OFF/NC");
                    wfdNetworkInterface = null;
                    rbMulticastNetIntWFD.setVisibility(View.GONE);
                    p2pLastKnownGroup = null;
                    return;
                }

                // group available: device connected

                // update global vars and GUI
                wfdNetworkInterface = AndroidUtils.getNetworkInterface(group.getInterface());
                rbMulticastNetIntWFD.setVisibility(View.VISIBLE);

                // update GUI with info message
//                tvWFDState.setText("WFD:  (" + (group.isGroupOwner() ? "GO" : group.getOwner().deviceName) +
//                        ")  " + group.getInterface() + "  " +
//                        WiFiDirectControlActivity.getLocalIpAddress(group.getInterface()) + " " + (group.isGroupOwner() ?
//                        ", clients: " + getClients(group) : "MyGO " + group.getOwner().deviceAddress + " " +
//                        SystemInfo.getIPFromMac(group.getOwner().deviceAddress)));

                p2pLastKnownGroup = group;
                updateWFDInterfaceInfo();
            }
        };

        // FD info listener
        NetworkInterfacesDetector.WFNetworkInterfaceListener wfNetworkInterfaceListener = new NetworkInterfacesDetector.WFNetworkInterfaceListener() {
            public void updateWFNetworkInterface(NetworkInterface wfInterface) {
                wfNetworkInterface = wfInterface;

                updateWFInterfaceInfo(wfInterface);
            }
        };

        // create network interfaces detector
        networkInterfacesDetector = new NetworkInterfacesDetector(this, wfdGroupInfoListener,
                wfNetworkInterfaceListener);

        //        updateNetworkInterfaces();
        networkInterfacesDetector.updateNetworkInterfaces();

        // execute init log dir system but after 2 seconds, it initially is not available
//        ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
//        worker.schedule(new Runnable() {
//            public void run() {
//                initLogDirSystem();
//            }
//        }, 2, TimeUnit.SECONDS);

        // Process task string (from file passed as argument, used in ADB sessions) if exists
        String taskStr = intent.getStringExtra("taskStr");
        if (taskStr != null)
            processTaskStr(taskStr);
    }

    /**
     *
     */
    private void btnSendStartTest_OnClickListener() {
        btnSendStartTest.setVisibility(View.GONE);
        btnCancelStartTest.setVisibility(View.GONE);

        Log.d(TAG, "Start test");

        // process start test msg: start N61 N62 1
        processNewLogDir(etLogDir.getText().toString().trim() + " " + deviceName + "-" + nextStartTestID++, null);

        // write current log dir
        etLogDir.setText(logDir);
        etLogDir.setEnabled(false);

        // avoid keyboard popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        etLogDir.setBackgroundColor(0xff800000);
    }

    /**
     *
     */
    private void activateLogDirSystem() {
        initLogDirSystem();
        btnActivateLogDirSystem.setVisibility(View.GONE);
        btnChangeLogDir.setVisibility(View.VISIBLE);
        btnUpdateLogDirReceiver.setVisibility(View.VISIBLE);
        btnDeactivateLogDirSystem.setVisibility(View.VISIBLE);
        btnEditStartTest.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void deactivateLogDirSystem() {
        stopLogDirSystem();
        btnActivateLogDirSystem.setVisibility(View.VISIBLE);
        btnChangeLogDir.setVisibility(View.GONE);
        btnUpdateLogDirReceiver.setVisibility(View.GONE);
        btnDeactivateLogDirSystem.setVisibility(View.GONE);
        btnEditStartTest.setVisibility(View.GONE);
    }


    /**
     *
     */
    public void requestNetworkWF() {
        requestNetworkWFAndExecuteOnAvailable(null);
    }

    /**
     *
     */
    public void requestNetworkWFAndExecuteOnAvailable(final Runnable task) {

        Log.d(TAG, "Bind to Network: WF; calling requestNetwork with TRANSPORT: WF");

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        NetworkRequest networkRequest = builder.build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

            public void onAvailable(Network network) {
                Log.d(TAG, "Network callback, network TRANSPORT: WF, available on " + network);

                networkWifi = network;
                if (task != null)
                    task.run();
            }

            public void onLosing(Network network, int maxMsToLive) {
                Log.d(TAG, "Network callback, network TRANSPORT: WF, lost on " + network);
            }
        };

        conManager.requestNetwork(networkRequest, networkCallback);
    }

    /**
     *
     */
    public void requestNetworkWFD() {

        Log.d(TAG, "Bind to Network: WFD; calling requestNetwork with CAPABILITY: P2P");

        // TODO fix this
        Log.d(TAG, "BIND to WF Direct not working on Nexus 6, 9 and OPO #######################################");

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P);
        //builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        NetworkRequest networkRequest = builder.build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            public void onAvailable(Network network) {
                Log.d(TAG, "Network callback, network with capability P2P/WFD, available on " + network);

                networkWifiDirect = network;
            }

            public void onLosing(Network network, int maxMsToLive) {
                Log.d(TAG, "Network callback, network TRANSPORT: WFD, lost on " + network);
            }
        };

        conManager.requestNetwork(networkRequest, networkCallback);
    }

    /**
     *
     */
    private void setTcpCommunicationMode() {
        // change to TCP mode
        communicationMode = COMM_MODE.TCP;
        btnUdpMulticast.setVisibility(View.GONE);
        btnTcp.setVisibility(View.VISIBLE);
        setVisibleReceptionReplyMode(true);
        btnSendImage.setEnabled(true);
        llMulticastNetworkInterfaces.setVisibility(View.GONE);
        tvCRAddress.setText("CR Address:");

        editTextCrIpAddress.setText(INITIAL_TCP_UDP_IPADDRESS.substring(0,
                INITIAL_TCP_UDP_IPADDRESS.lastIndexOf('.')));
        editTextCrIpAddressLastPart.setText(INITIAL_TCP_UDP_IPADDRESS.substring(
                INITIAL_TCP_UDP_IPADDRESS.lastIndexOf('.') + 1));

        editTextCrPortNumber.setText(String.valueOf(INITIAL_TCP_UDP_PORT));
        tvReceptionAddress.setText("Receive Port Number: ");
        etMulticastRcvIpAddress.setVisibility(View.GONE);
        etReceivePortNumber.setText(String.valueOf(INITIAL_TCP_UDP_PORT));

        Log.d(TAG, "Multicast lock released");
        wifiMulticastLock.release();
    }

    /* *
     *
     */
    private void setUdpCommunicationMode() {
        // change to UDP mode
        communicationMode = COMM_MODE.UDP;
        btnTcp.setVisibility(View.GONE);
        btnUdp.setVisibility(View.VISIBLE);
        setVisibleReceptionReplyMode(false);
        btnSendImage.setEnabled(false);
    }

    /* *
    *
    */
    private void setUdpMulticastCommunicationMode() {
        // change to UDP Multicast mode
        communicationMode = COMM_MODE.UDP_MULTICAST;
        btnUdp.setVisibility(View.GONE);
        btnUdpMulticast.setVisibility(View.VISIBLE);

        setVisibleReceptionReplyMode(false);
        btnSendImage.setEnabled(false);
        llMulticastNetworkInterfaces.setVisibility(View.VISIBLE);
        tvCRAddress.setText("MCR Address:");

        editTextCrIpAddress.setText(INITIAL_MULTICAST_UDP_IPADDRESS.substring(0,
                INITIAL_MULTICAST_UDP_IPADDRESS.lastIndexOf('.')));
        editTextCrIpAddressLastPart.setText(INITIAL_MULTICAST_UDP_IPADDRESS.substring(
                INITIAL_MULTICAST_UDP_IPADDRESS.lastIndexOf('.') + 1));

        editTextCrPortNumber.setText(String.valueOf(INITIAL_MULTICAST_UDP_PORT));
        tvReceptionAddress.setText("Multicast Address: ");
        etMulticastRcvIpAddress.setVisibility(View.VISIBLE);
        etReceivePortNumber.setText(String.valueOf(INITIAL_MULTICAST_UDP_PORT));

        // only acquired when multicast enabled, because I suspect that multicast active is slowing down communications
        Log.d(TAG, "Multicast lock acquired");
        wifiMulticastLock.acquire();
    }

    /**
     *
     */
    private void processNewLogDir(final String newLogDir, final NetworkInterface networkInterfaceReceivedData) {
        if (logDir.equalsIgnoreCase(newLogDir))
            return;

        if (!newLogDir.startsWith("start ")) {
            // new log dir
            logDir = newLogDir;
            Log.d(TAG, "Processing a new log dir: " + newLogDir);
            etLogDir.post(new Runnable() {
                @Override
                public void run() {
                    etLogDir.setText(logDir);
                }
            });

        } else {
            // start test msg
            Log.d(TAG, "Processing a new Start test msg: " + newLogDir);

            if (lastStartTestMsg.equalsIgnoreCase(newLogDir))
                return;
            lastStartTestMsg = newLogDir;

            // check if it is not for me
            if (!newLogDir.contains(deviceName)) {
                Log.d(TAG, newLogDir + " ignored");
                return;
            }

            // wait for 1 one second close logDir System
            guiThreadLoopHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Start test: Terminating logDirSystem");
                    deactivateLogDirSystem();
                }
            }, 1000);

            // in 5 seconds start transmitting
            guiThreadLoopHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AndroidUtils.toast(ClientActivity.this, "Start test: Start test");

                    // start transmitting
                    startTransmittingGuiActions(false);
                    doTransmit(null);   // send dummy data for tests

                    numberOfTransmittingTestsToDo = numberOfCurrentTest = 1;
                }
            }, 5000);
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                // get multicast socket, in a dynamic port
                try {
                    InetAddress multicastLogDirInetAddress = InetAddress.getByName(multicastLogDirIpAddress);

                    byte buffer[] = new byte[1024];

                    // first 4 bytes contains the string length
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putInt(newLogDir.length());
                    System.arraycopy(buf.array(), 0, buffer, 0, 4);

                    // write message at index 4
                    System.arraycopy(newLogDir.getBytes(), 0, buffer, 4, newLogDir.getBytes().length);

                    // build packet and send it
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastLogDirInetAddress, multicastLogDirPort);

                    Log.d(TAG, "will send new dir to connected interfaces: " + newLogDir);

                    if (networkInterfaceReceivedData == null || networkInterfaceReceivedData == wfNetworkInterface)
                        sendLogDirMessageThroughInterface(packet, wfdNetworkInterface, newLogDir);

                    if (networkInterfaceReceivedData == null || networkInterfaceReceivedData == wfdNetworkInterface)
                        sendLogDirMessageThroughInterface(packet, wfNetworkInterface, newLogDir);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     *
     */
    private void sendLogDirMessageThroughInterface(DatagramPacket packet, NetworkInterface netInterface, String newLogDir) {
        if (netInterface == null)
            return;

        try {

            Log.d(TAG, "Sending new dir " + newLogDir + " to interface: " + netInterface.getName());

            MulticastSocket txLogDirSocket = new MulticastSocket();
            txLogDirSocket.setTimeToLive(20);
            txLogDirSocket.setNetworkInterface(netInterface);
            txLogDirSocket.send(packet);
            txLogDirSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * just close multicast sockets
     */
    private void updateLogDirReceiver() {
        Log.d(TAG, "Update log dir receiver...");

        if (logDirMulticastDataWFD.multicastSocket != null) {
            logDirMulticastDataWFD.multicastSocket.close();
        } else if (wfdNetworkInterface != null)
            logDirStartReceivingMulticastInterface(logDirMulticastDataWFD);


        if (logDirMulticastDataWF.multicastSocket != null) {
            logDirMulticastDataWF.multicastSocket.close();
        } else if (wfNetworkInterface != null)
            logDirStartReceivingMulticastInterface(logDirMulticastDataWF);
    }

    /**
     *
     */
    private void initLogDirSystem() {
        logDirMulticastDataWFD = new LogDirMulticastSocketContainer(true);
        logDirMulticastDataWF = new LogDirMulticastSocketContainer(false);

        if (wfdNetworkInterface != null)
            logDirStartReceivingMulticastInterface(logDirMulticastDataWFD);

        if (wfNetworkInterface != null)
            logDirStartReceivingMulticastInterface(logDirMulticastDataWF);
    }

    /**
     *
     */
    private void stopLogDirSystem() {
        if (logDirMulticastDataWFD != null)
            logDirMulticastDataWFD.stopReceiving();

        if (logDirMulticastDataWF != null)
            logDirMulticastDataWF.stopReceiving();

        logDirMulticastDataWFD = logDirMulticastDataWF = null;
    }

    /**
     *
     */
    private void logDirStartReceivingMulticastInterface(final LogDirMulticastSocketContainer logDirMulticastContainer) {
        if (logDirMulticastContainer.getNetworkInterface() == null)
            return;


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        try {

                            NetworkInterface networkInterface = logDirMulticastContainer.getNetworkInterface();
                            if (networkInterface == null) {
                                logDirMulticastContainer.multicastSocket = null;
                                logDirMulticastContainer.thread = null;
                                break;
                            }

                            // get multicast socket in a local port - must be the same
                            MulticastSocket multicastReceiverSocket = new MulticastSocket(multicastLogDirPort);

                            // keep multicast socket to enable closing it from outside
                            logDirMulticastContainer.multicastSocket = multicastReceiverSocket;

                            // create multicast socket endpoint
                            InetSocketAddress iSock = new InetSocketAddress(multicastLogDirIpAddress, multicastLogDirPort);

                            // joint multicast group defined by the multicast endpoint with the received interface
                            Log.d(TAG, "Multicast Log Dir socket is registering in " + networkInterface.getName());
                            multicastReceiverSocket.joinGroup(iSock, networkInterface);

                            byte[] buffer = new byte[1024];
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                            while (true) {
                                // wait for datagrams
                                multicastReceiverSocket.receive(packet);

                                byte[] bufferRcv = packet.getData();
                                String newLogDir = new String(bufferRcv, 4, ByteBuffer.wrap(bufferRcv, 0, 4).getInt());

                                Log.d(TAG, "Received new log dir: " + newLogDir + " from " +
                                        packet.getAddress().toString().substring(1));

                                processNewLogDir(newLogDir, networkInterface);
                            }

                        } catch (SocketException e) {

                        }
                        // check if to terminate thread
                        if (Thread.currentThread().isInterrupted()) {
                            Log.d(TAG, "Log dir receiving thread will die");
                            return;
                        }

                        Log.d(TAG, "Log dir receiving thread will renew multicast receiver subscription");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Log dir receiving thread is dying");
            }

        });

        thread.start();
        logDirMulticastContainer.thread = thread;
    }

    /**
     *
     */
    private void stopReceivingServerActions() {
        // stop receiving server
        clientReceiver.stopThread();
        clientReceiver = null;
        endReceivingGuiActions();
    }

    /**
     *
     */
    private void clearReceptionLogs() {
        for (Iterator<ReceptionGuiInfo> it = receptionGuiInfos.iterator(); it.hasNext(); ) {
            ReceptionGuiInfo rgi = it.next();
            if (rgi.isTerminated()) {
                llReceptionLogs.removeView(rgi.getView());
                it.remove();
            }
        }
    }

    /**
     *
     */
    private void updateWFInterfaceInfo(NetworkInterface wfInterface) {
        String baseStr = "WF " + (configurations.isPriorityInterfaceWF() ? "(P)" : "") + ":  ";
        if (wfInterface == null) {
            tvWFState.setText(baseStr + "OFF/NC");
            rbMulticastNetIntWF.setVisibility(View.GONE);
        } else {
            tvWFState.setText(baseStr + AndroidUtils.getWifiSSID(ClientActivity.this) + "  " +
                    wfInterface.getName() + "  " +
                    AndroidUtils.getWifiLinkSpeed(ClientActivity.this) + "Mbps  " +
                    getIPV4AddressWithBroadcast(wfInterface).toString().substring(1));
            rbMulticastNetIntWF.setVisibility(View.VISIBLE);
        }
    }

    /**
     *
     */
    private void updateWFDInterfaceInfo() {
        String baseStr = "WFD " + (configurations.isPriorityInterfaceWFD() ? "(P)" : "") + ":  ";
        if (p2pLastKnownGroup == null)
            tvWFDState.setText(baseStr + "OFF/NC");
        else if (p2pLastKnownGroup.isGroupOwner()) {
            //Log.d("NETINT", AndroidUtils.getHostName());  // net hostname, not wfd device name
            // Group owner
            tvWFDState.setText(baseStr + "(GO)  " + p2pLastKnownGroup.getInterface() + "  " +
                    WiFiDirectControlActivity.getLocalIpAddress(p2pLastKnownGroup.getInterface()) + " " +
                    // p2pLastKnownGroup.getOwner().deviceAddress + " " +  // this method gives a different value
                    SystemInfo.getInterfaceMacAddress(p2pLastKnownGroup.getInterface()) +
                    ", clients: " + getClients(p2pLastKnownGroup));
        } else {
            // CLIENT: The GO IP address could not be obtained from arp file
            //String myMacAddressOnWFDInterface = SystemInfo.getInterfaceMacAddress(p2pLastKnownGroup.getInterface());
            // Log.d("NETINT", AndroidUtils.getHostName()); // net hostname, not wfd device name

            tvWFDState.setText(baseStr +
                    p2pLastKnownGroup.getInterface() + "  " +
                    WiFiDirectControlActivity.getLocalIpAddress(p2pLastKnownGroup.getInterface()) + " " +
                    //SystemInfo.getInterfaceMacAddress(p2pLastKnownGroup.getInterface()) + " " +  // this method gives a different value
                    SystemInfo.getInterfaceMacAddress(p2pLastKnownGroup.getInterface()) + ", MyGO: " +
                    p2pLastKnownGroup.getOwner().deviceName + " " +
                    networkInterfacesDetector.getGoAddress().toString().substring(1) + " " +
                    p2pLastKnownGroup.getOwner().deviceAddress);
        }
    }

    /**
     *
     */
    public static String getClients(WifiP2pGroup group) {
        StringBuilder s = new StringBuilder().append("[ ");
        Collection<WifiP2pDevice> devList = group.getClientList();
        boolean firstItem = true;
        for (WifiP2pDevice dev : devList) {
            if (firstItem)
                firstItem = false;
            else s.append(", ");
            s.append(dev.deviceName).append(" ").append(SystemInfo.getIPFromMac(dev.deviceAddress));
            s.append(" ").append(dev.deviceAddress);
        }
        return s.append("]").toString();
    }

    /**
     * GUI action to transmit data over TCP, UDP or UDP multicast
     */
    private void doTransmit(Uri fileToSend) {
        String crIpAddress = editTextCrIpAddress.getText().toString() + "." + editTextCrIpAddressLastPart.getText().toString();
        int crPortNumber = Integer.parseInt(editTextCrPortNumber.getText().toString());

        String destIpAddress = editTextDestIpAddress.getText().toString();
        int destPortNumber = Integer.parseInt(editTextDestPortNumber.getText().toString());

        long totalKBytesToSend = Long.parseLong(editTextTotalBytesToSend.getText().toString());
        if (btnSentMBytes.isShown())
            totalKBytesToSend *= 1024;
        long delayMs = Long.parseLong(editTextDelay.getText().toString());
        int bufferSizeBytes = 1024 * Integer.parseInt(editTextMaxBufferSize.getText().toString());

        transmitData(fileToSend, crIpAddress, crPortNumber, destIpAddress, destPortNumber, bufferSizeBytes, delayMs, totalKBytesToSend, currentBindToNetwork);
    }


    /**
     *
     */
    private void processTaskStr(String taskStr) {
        Log.d(TAG, "TaskString: " + taskStr);
        final HashMap<String, String> map = MainActivity.getParamsMap(taskStr);

        // communication mode: mode
        final String commMode = map.get("mode");
        if (commMode == null || !(commMode.equalsIgnoreCase("tcp") || commMode.equalsIgnoreCase("udp")
                || commMode.equalsIgnoreCase("udpMulticast")))
            throw new IllegalStateException("Client activity, received invalid communication mode parameter: " + commMode);

        // receive or transmit action: action
        final String action = map.get("action");
        if (action == null || !(action.equalsIgnoreCase("receive") || action.equalsIgnoreCase("transmit")))
            throw new IllegalStateException("Client activity, received invalid action parameter: " + action);

        // log directory: logDirectory
        logDir = map.get("logDirectory");
        etLogDir.setText(logDir);

        // get bindSocketToNetwork
        final String bindSocketToNetwork = map.get("bindSocketToNetwork");
        if(bindSocketToNetwork != null)
            btnBindToNetwork.setText("Bind to " + bindSocketToNetwork.toUpperCase());

        // run with some delay to give time to stabilize network interfaces, need mostly by UDP multicast
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                // TCP
                if (commMode.equalsIgnoreCase("tcp")) {
                    communicationMode = COMM_MODE.TCP;
                    if (action.equalsIgnoreCase("receive"))
                        processTCPReceiveAction(map);

                    else {
                        // transmit
                        if (bindSocketToNetwork != null && bindSocketToNetwork.equalsIgnoreCase("WF")) {
                            requestNetworkWFAndExecuteOnAvailable(new Runnable() {
                                @Override
                                public void run() {
                                    processTCPTransmitAction(map);
                                }
                            });
                        } else processTCPTransmitAction(map);
                    }
                }
                // UDP
                else if (commMode.equalsIgnoreCase("udp")) {
                    communicationMode = COMM_MODE.UDP;
                    if (action.equalsIgnoreCase("receive"))
                        processUDPReceiveAction(map);

                    else {
                        // transmit
                        if (bindSocketToNetwork != null && bindSocketToNetwork.equalsIgnoreCase("WF")) {
                            requestNetworkWFAndExecuteOnAvailable(new Runnable() {
                                @Override
                                public void run() {
                                    processUDPTransmitAction(map);
                                }
                            });
                        } else processUDPTransmitAction(map);
                    }
                }
                // udpMultitask
                else if (commMode.equalsIgnoreCase("udpMulticast")) {
                    communicationMode = COMM_MODE.UDP_MULTICAST;
                    if (action.equalsIgnoreCase("receive")) {
                        processUDPMulticastReceiveAction(map);
                    } else
                        processUDPMulticastTransmitAction(map);
                }

            }
        }, 500);

    }


    /**
     *
     */
    private void processTCPReceiveAction(HashMap<String, String> map) {
        // receive port number: ReceivePort
        String rcvPortNumberStr = map.get("receivePort");
        if (rcvPortNumberStr == null)
            throw new IllegalStateException("Client activity, received no receive port number");
        int rcvPortNumber = Integer.parseInt(rcvPortNumberStr);

        // buffer size: BufferKB
        String bufferSizeKBStr = map.get("bufferKB");
        int bufferSizeKB = bufferSizeKBStr == null ? 1 : Integer.parseInt(bufferSizeKBStr);

        // reply mode: ReplyMode
        String replyModeStr = map.get("replyMode");
        if (replyModeStr == null || !(replyModeStr.equalsIgnoreCase("None") || replyModeStr.equalsIgnoreCase("Echo") ||
                replyModeStr.equalsIgnoreCase("Ok")))
            throw new IllegalStateException("Client activity, received invalid replyMode: " + replyModeStr);
        ReplyMode replyMode = replyModeStr.equalsIgnoreCase("None") ? ReplyMode.NONE :
                replyModeStr.equalsIgnoreCase("Echo") ? ReplyMode.ECHO : ReplyMode.OK;

        if (replyMode == ReplyMode.ECHO) rbReplyInfoEcho.setChecked(true);
        if (replyMode == ReplyMode.OK) rbReplyInfoOKs.setChecked(true);

        Log.d(TAG, "TCP receive, with: rcvPortNumber = " + rcvPortNumber + ", bufferKB = " + bufferSizeKB +
                ", replyMode = " + replyMode);

        // update GUI
        startReceivingGuiActions();

        // start receiving
        clientReceiver = new ClientDataReceiverServerSocketThreadTCP(rcvPortNumber, llReceptionLogs,
                bufferSizeKB * 1024, replyMode, this);
        clientReceiver.start();
    }

    /**
     *
     */
    private void processUDPReceiveAction(HashMap<String, String> map) {

        setUdpCommunicationMode();

        // receive port number: ReceivePort
        String rcvPortNumberStr = map.get("receivePort");
        if (rcvPortNumberStr == null)
            throw new IllegalStateException("Client activity, received no receive port number");
        int rcvPortNumber = Integer.parseInt(rcvPortNumberStr);

        // buffer size: BufferKB
        String bufferSizeKBStr = map.get("bufferKB");
        int bufferSizeKB = bufferSizeKBStr == null ? 1 : Integer.parseInt(bufferSizeKBStr);

        Log.d(TAG, "UDP receive, with: rcvPortNumber = " + rcvPortNumber + ", bufferKB = " + bufferSizeKB);

        // update GUI
        startReceivingGuiActions();


        // start receiving, build the UDP receiver socket
        try {
            UDPSocket sock = UDPSocket.getUDPReceiverSocket(rcvPortNumber);

            // create worker thread receiving in UDP socket
            clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                    sock, llReceptionLogs, bufferSizeKB * 1024, this);

            clientReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void processUDPMulticastReceiveAction(HashMap<String, String> map) {

        btnTcp.setVisibility(View.GONE);
        setUdpMulticastCommunicationMode();

        // receive multicast address: receiveMulticastAddress
        String multicastNetworkInterface = map.get("multicastNetworkInterface");
        if (multicastNetworkInterface == null ||
                !(multicastNetworkInterface.equalsIgnoreCase("WF") || multicastNetworkInterface.equalsIgnoreCase("WFD")))
            throw new IllegalStateException("Client activity, received without a valid Multicast Network Interface:" +
                    multicastNetworkInterface);
        if (multicastNetworkInterface.equalsIgnoreCase("WF"))
            rbMulticastNetIntWF.setChecked(true);
        if (multicastNetworkInterface.equalsIgnoreCase("WFD"))
            rbMulticastNetIntWFD.setChecked(true);

        // receive multicast address: multicastAddress
        String rcvMulticastAddressStr = map.get("multicastAddress");
        if (rcvMulticastAddressStr == null)
            throw new IllegalStateException("Client activity, receive without multicast address");
        etMulticastRcvIpAddress.setText(rcvMulticastAddressStr);


        // receive multicast port number: receiveMulticastPort
        String rcvMulticastPortNumberStr = map.get("multicastPort");
        if (rcvMulticastPortNumberStr == null)
            throw new IllegalStateException("Client activity, receive without multicast port number");
        int rcvMulticastPortNumber = Integer.parseInt(rcvMulticastPortNumberStr);
        etReceivePortNumber.setText(rcvMulticastPortNumberStr);

        // buffer size: BufferKB
        String bufferSizeKBStr = map.get("bufferKB");
        int bufferSizeKB = bufferSizeKBStr == null ? 1 : Integer.parseInt(bufferSizeKBStr);

        Log.d(TAG, "UDP multicast receive: mc address = " + rcvMulticastAddressStr +
                ", mc portNumber = " + rcvMulticastPortNumber +
                ", mc netInterface = " + multicastNetworkInterface + ", bufferKB = " + bufferSizeKB);

        // update GUI
        startReceivingGuiActions();

        // start receiving, build the UDP receiver socket
        try {
            NetworkInterface netInterface = rbMulticastNetIntWFD.isChecked() ? wfdNetworkInterface :
                    rbMulticastNetIntWF.isChecked() ? wfNetworkInterface : null;

            // build the UDP receiver socket
            UDPSocket msock = UDPSocket.getUDPMulticastReceiverSocket(
                    rcvMulticastAddressStr, rcvMulticastPortNumber, netInterface);

            // create worker thread receiving in UDP socket
            clientReceiver = new ClientDataReceiverServerSocketThreadUDP(
                    msock, llReceptionLogs, bufferSizeKB * 1024, this);

            clientReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void processTCPTransmitAction(HashMap<String, String> map) {

        // CR IP address: crAddress = 192.168.49.1
        final String crIpAddress = map.get("crAddress");
        if (crIpAddress == null)
            throw new IllegalStateException("Client activity, transmit with no CR IP address");

        // CR port number: crPort
        String crPortNumberStr = map.get("crPort");
        if (crPortNumberStr == null)
            throw new IllegalStateException("Client activity, transmit with no cr port number");
        final int crPortNumber = Integer.parseInt(crPortNumberStr);

        // Destination IP address: DestAddress = Rt
        final String destIpAddress = map.get("destAddress");
        if (destIpAddress == null)
            throw new IllegalStateException("Client activity, transmit with no DestAddress");

        // Destination port number: destPort
        String destPortNumberStr = map.get("destPort");
        if (destPortNumberStr == null)
            throw new IllegalStateException("Client activity, transmit with no dest port number");
        final int destPortNumber = Integer.parseInt(destPortNumberStr);

        //  bindSocketToNetwork
        BIND_TO_NETWORK bindToNetwork = BIND_TO_NETWORK.NONE;
        String bindSocketToNetworkStr = map.get("bindSocketToNetwork");
        if (bindSocketToNetworkStr != null) {
            if (bindSocketToNetworkStr.equalsIgnoreCase("WF"))
                bindToNetwork = BIND_TO_NETWORK.WF;
            else if (bindSocketToNetworkStr.equalsIgnoreCase("WFD"))
                bindToNetwork = BIND_TO_NETWORK.WFD;
            else throw new IllegalStateException("Client activity, transmit bind to network with invalid value: " +
                        bindSocketToNetworkStr);
        } else bindToNetwork = BIND_TO_NETWORK.NONE;
        final BIND_TO_NETWORK finalBindToNetwork = bindToNetwork;

        // buffer size: BufferKB
        String bufferSizeKBStr = map.get("bufferKB");
        final int bufferSizeKB = bufferSizeKBStr == null ? 1 : Integer.parseInt(bufferSizeKBStr);

        // delays: delayMs
        String delayMsStr = map.get("delayMs");
        final int delayMs = delayMsStr == null ? 0 : Integer.parseInt(delayMsStr);

        // total bytes to send: totalBytesToSend=100MB  100KB
        String totalBytesToSendStr = map.get("totalBytesToSend");
        if (totalBytesToSendStr == null || !(totalBytesToSendStr.endsWith("MB") || totalBytesToSendStr.endsWith("KB")))
            throw new IllegalStateException("Client activity, transmit with no correct total bytes to send: " + totalBytesToSendStr);
        final int totalKBToSend = Integer.parseInt(totalBytesToSendStr.substring(0, totalBytesToSendStr.length() - 2)) *
                (totalBytesToSendStr.endsWith("MB") ? 1024 : 1);

        // number of tests: numberOfTests
        String numberOfTestsStr = map.get("numberOfTests");
        numberOfTransmittingTestsToDo = numberOfTestsStr == null ? 1 : Integer.parseInt(numberOfTestsStr);

        // delay before each test in MS: delayBeforeEachTestMS=1000
        String delayBeforeEachTestMSStr = map.get("delayBeforeEachTestMS");
        delayBeforeEachTransmitTestMS = delayBeforeEachTestMSStr == null ? 0 : Integer.parseInt(delayBeforeEachTestMSStr);

        // screen state on tests : screenOnTests = on / off
        String screenOnTestsStr = map.get("screenOnTests");
        boolean screenONOnTests = delayBeforeEachTestMSStr == null || Boolean.parseBoolean(screenOnTestsStr);

        Log.d(TAG, "TCP transmit, with: crIpAddress = " + crIpAddress + ", crPortNumber = " + crPortNumber +
                ", destIpAddress = " + destIpAddress + ", destPortNumber = " + destPortNumber +
                ", bind to network = " + bindToNetwork + ", bufferSizeKB = " + bufferSizeKB +
                ", delayMs = " + delayMs + ", totalBytesToSend = " + totalKBToSend +
                ", with numberOfTests = " + numberOfTransmittingTestsToDo +
                ", delay BeforeEachTestMs = " + delayBeforeEachTestMSStr + ", screenOnTests = " + screenOnTestsStr);


        // transmit timer task
        transmitTask = new Runnable() {
            public void run() {
                // update gui
                btnSendData.post(new Runnable() {
                    public void run() {
                        startTransmittingGuiActions(false);
                        //alertDialogScreenOff.cancel();
                    }
                });

                doNotificationStart();

                Log.d(TAG, "Will run test nº: " + numberOfCurrentTest + " of " + numberOfTransmittingTestsToDo);

                // start transmitting
                transmitData(null, crIpAddress, crPortNumber, destIpAddress, destPortNumber, bufferSizeKB * 1024, delayMs,
                        totalKBToSend, finalBindToNetwork); // send dummy data for tests
            }
        };

        // create transmit handler and schedule transmit task, count with one less transmit task
        transmitHandler = new Handler();

//        if (!screenONOnTests && ((PowerManager) getSystemService(Context.POWER_SERVICE)).isInteractive()) {
//            // show alert dialog to wait for screen off
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            alertDialogScreenOff = builder.setTitle("Screen off please").setMessage("Turn screen off").create();
//            alertDialogScreenOff.show();
//
//            // wait for screen off and then start counting
//            runAfterScreenOff(new Runnable() {
//                public void run() {
//                    transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
//                }
//            });
//
//        } else {
        transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
//        }
    }

    /**
     *
     */
    private void processUDPTransmitAction(HashMap<String, String> map) {

        btnSendData.post(new Runnable() {
            public void run() {
                setUdpCommunicationMode();
            }
        });


        // CR IP address: crAddress = 192.168.49.1
        final String crIpAddress = map.get("crAddress");
        if (crIpAddress == null)
            throw new IllegalStateException("Client activity, transmit with no CR IP address");

        // CR port number: crPort
        String crPortNumberStr = map.get("crPort");
        if (crPortNumberStr == null)
            throw new IllegalStateException("Client activity, transmit with no cr port number");
        final int crPortNumber = Integer.parseInt(crPortNumberStr);

        // Destination IP address: DestAddress = Rt
        final String destIpAddress = map.get("destAddress");
        if (destIpAddress == null)
            throw new IllegalStateException("Client activity, transmit with no DestAddress");

        // Destination port number: destPort
        String destPortNumberStr = map.get("destPort");
        if (destPortNumberStr == null)
            throw new IllegalStateException("Client activity, transmit with no dest port number");
        final int destPortNumber = Integer.parseInt(destPortNumberStr);

        //  bindSocketToNetwork
        BIND_TO_NETWORK bindToNetwork = BIND_TO_NETWORK.NONE;
        String bindSocketToNetworkStr = map.get("bindSocketToNetwork");
        if (bindSocketToNetworkStr != null) {
            if (bindSocketToNetworkStr.equalsIgnoreCase("WF"))
                bindToNetwork = BIND_TO_NETWORK.WF;
            else if (bindSocketToNetworkStr.equalsIgnoreCase("WFD"))
                bindToNetwork = BIND_TO_NETWORK.WFD;
            else throw new IllegalStateException("Client activity, transmit bind to network with invalid value: " +
                        bindSocketToNetworkStr);
        } else bindToNetwork = BIND_TO_NETWORK.NONE;
        final BIND_TO_NETWORK finalBindToNetwork = bindToNetwork;

        // buffer size: BufferKB
        String bufferSizeKBStr = map.get("bufferKB");
        final int bufferSizeKB = bufferSizeKBStr == null ? 1 : Integer.parseInt(bufferSizeKBStr);

        // delays: delayMs
        String delayMsStr = map.get("delayMs");
        final int delayMs = delayMsStr == null ? 0 : Integer.parseInt(delayMsStr);

        // total bytes to send: totalBytesToSend=100MB  100KB
        String totalBytesToSendStr = map.get("totalBytesToSend");
        if (totalBytesToSendStr == null || !(totalBytesToSendStr.endsWith("MB") || totalBytesToSendStr.endsWith("KB")))
            throw new IllegalStateException("Client activity, transmit with no correct total bytes to send: " + totalBytesToSendStr);
        final int totalKBToSend = Integer.parseInt(totalBytesToSendStr.substring(0, totalBytesToSendStr.length() - 2)) *
                (totalBytesToSendStr.endsWith("MB") ? 1024 : 1);

        // number of tests: numberOfTests
        String numberOfTestsStr = map.get("numberOfTests");
        numberOfTransmittingTestsToDo = numberOfTestsStr == null ? 1 : Integer.parseInt(numberOfTestsStr);

        // delay before each test in MS: delayBeforeEachTestMS=1000
        String delayBeforeEachTestMSStr = map.get("delayBeforeEachTestMS");
        delayBeforeEachTransmitTestMS = delayBeforeEachTestMSStr == null ? 0 : Integer.parseInt(delayBeforeEachTestMSStr);

        // screen state on tests : screenOnTests = on / off
        String screenOnTestsStr = map.get("screenOnTests");
        boolean screenONOnTests = delayBeforeEachTestMSStr == null || Boolean.parseBoolean(screenOnTestsStr);

        Log.d(TAG, "UDP transmit, with: crIpAddress = " + crIpAddress + ", crPortNumber = " + crPortNumber +
                ", destIpAddress = " + destIpAddress + ", destPortNumber = " + destPortNumber +
                ", bind to network = " + bindToNetwork + ", bufferSizeKB = " + bufferSizeKB +
                ", delayMs = " + delayMs + ", totalBytesToSend = " + totalKBToSend +
                ", with numberOfTests = " + numberOfTransmittingTestsToDo +
                ", delay BeforeEachTestMs = " + delayBeforeEachTestMSStr + ", screenOnTests = " + screenOnTestsStr);


        // transmit timer task
        transmitTask = new Runnable() {
            public void run() {
                // update gui
                btnSendData.post(new Runnable() {
                    public void run() {
                        startTransmittingGuiActions(false);
                        //alertDialogScreenOff.cancel();
                    }
                });

                doNotificationStart();

                Log.d(TAG, "Will run test nº: " + numberOfCurrentTest + " of " + numberOfTransmittingTestsToDo);

                // start transmitting
                transmitData(null, crIpAddress, crPortNumber, destIpAddress, destPortNumber, bufferSizeKB * 1024, delayMs,
                        totalKBToSend, finalBindToNetwork); // send dummy data for tests
            }
        };

        // create transmit handler and schedule transmit task, count with one less transmit task
        transmitHandler = new Handler();


        // if screen should be off and it is on wait until user turns it off
//        if (!screenONOnTests && ((PowerManager) getSystemService(Context.POWER_SERVICE)).isInteractive()) {
//            // show alert dialog to wait for screen off
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            alertDialogScreenOff = builder.setTitle("Screen off please").setMessage("Turn screen off").create();
//            alertDialogScreenOff.show();
//
//            // wait for screen off and then start counting
//            runAfterScreenOff(new Runnable() {
//                public void run() {
//                    transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
//                }
//            });
//
//        } else {
        transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
//        }
    }

    /**
     *
     */
    private void processUDPMulticastTransmitAction(HashMap<String, String> map) {

        btnTcp.setVisibility(View.GONE);
        setUdpMulticastCommunicationMode();

        // transmit multicast address: receiveMulticastAddress
        String multicastNetworkInterface = map.get("multicastNetworkInterface");
        if (multicastNetworkInterface == null ||
                !(multicastNetworkInterface.equalsIgnoreCase("WF") || multicastNetworkInterface.equalsIgnoreCase("WFD")))
            throw new IllegalStateException("Client activity, transmit without a valid Multicast Network Interface:" +
                    multicastNetworkInterface);
        if (multicastNetworkInterface.equalsIgnoreCase("WF"))
            rbMulticastNetIntWF.setChecked(true);
        if (multicastNetworkInterface.equalsIgnoreCase("WFD"))
            rbMulticastNetIntWFD.setChecked(true);

        // multicast address: receiveMulticastAddress
        final String multicastAddressStr = map.get("multicastAddress");
        if (multicastAddressStr == null)
            throw new IllegalStateException("Client activity, transmit without multicast address");
        editTextCrIpAddress.setText(multicastAddressStr.substring(0, multicastAddressStr.lastIndexOf('.')));
        editTextCrIpAddressLastPart.setText(multicastAddressStr.substring(multicastAddressStr.lastIndexOf('.') + 1));

        // receive multicast port number: receiveMulticastPort
        String multicastPortNumberStr = map.get("multicastPort");
        if (multicastPortNumberStr == null)
            throw new IllegalStateException("Client activity, transmit without multicast port number");
        final int multicastPortNumber = Integer.parseInt(multicastPortNumberStr);
        editTextCrPortNumber.setText(multicastPortNumberStr);

        // Destination IP address: DestAddress = Rt
        final String destIpAddress = map.get("destAddress");
        if (destIpAddress == null)
            throw new IllegalStateException("Client activity, transmit with no DestAddress");

        // Destination port number: destPort
        String destPortNumberStr = map.get("destPort");
        if (destPortNumberStr == null)
            throw new IllegalStateException("Client activity, transmit with no dest port number");
        final int destPortNumber = Integer.parseInt(destPortNumberStr);

        // buffer size: BufferKB
        String bufferSizeKBStr = map.get("bufferKB");
        final int bufferSizeKB = bufferSizeKBStr == null ? 1 : Integer.parseInt(bufferSizeKBStr);

        // delays: delayMs
        String delayMsStr = map.get("delayMs");
        final int delayMs = delayMsStr == null ? 0 : Integer.parseInt(delayMsStr);

        // total bytes to send: totalBytesToSend=100MB  100KB
        String totalBytesToSendStr = map.get("totalBytesToSend");
        if (totalBytesToSendStr == null || !(totalBytesToSendStr.endsWith("MB") || totalBytesToSendStr.endsWith("KB")))
            throw new IllegalStateException("Client activity, transmit with no correct total bytes to send: " + totalBytesToSendStr);
        final int totalKBToSend = Integer.parseInt(totalBytesToSendStr.substring(0, totalBytesToSendStr.length() - 2)) *
                (totalBytesToSendStr.endsWith("MB") ? 1024 : 1);

        // number of tests: numberOfTests
        String numberOfTestsStr = map.get("numberOfTests");
        numberOfTransmittingTestsToDo = numberOfTestsStr == null ? 1 : Integer.parseInt(numberOfTestsStr);

        // delay before each test in MS: delayBeforeEachTestMS=1000
        String delayBeforeEachTestMSStr = map.get("delayBeforeEachTestMS");
        delayBeforeEachTransmitTestMS = delayBeforeEachTestMSStr == null ? 0 : Integer.parseInt(delayBeforeEachTestMSStr);

        // screen state on tests : screenOnTests = on / off
        String screenOnTestsStr = map.get("screenOnTests");
        boolean screenONOnTests = delayBeforeEachTestMSStr == null || Boolean.parseBoolean(screenOnTestsStr);

        Log.d(TAG, "UDP Multicast transmit: mc networkInterface = " + multicastNetworkInterface +
                ", mc Address = " + multicastAddressStr +
                ", mc portNumber = " + multicastPortNumber +
                ", destIpAddress = " + destIpAddress + ", destPortNumber = " + destPortNumber + ", bufferSizeKB = " +
                bufferSizeKB + ", delayMs = " + delayMs + ", totalBytesToSend = " + totalKBToSend +
                ", with numberOfTests = " + numberOfTransmittingTestsToDo +
                ", delay BeforeEachTestMs = " + delayBeforeEachTestMSStr + ", screenOnTests = " + screenOnTestsStr);

        // transmit timer task
        transmitTask = new Runnable() {
            public void run() {
                // update gui
                btnSendData.post(new Runnable() {
                    public void run() {
                        startTransmittingGuiActions(false);
                        //alertDialogScreenOff.cancel();
                    }
                });

                doNotificationStart();

                Log.d(TAG, "Will run test nº: " + numberOfCurrentTest + " of " + numberOfTransmittingTestsToDo);

                // start transmitting
                transmitData(null, multicastAddressStr, multicastPortNumber, destIpAddress, destPortNumber, bufferSizeKB * 1024, delayMs,
                        totalKBToSend, BIND_TO_NETWORK.NONE); // send dummy data for tests
            }
        };

        // create transmit handler and schedule transmit task, count with one less transmit task
        transmitHandler = new Handler();

        // if screen should be off and it is on wait until user turns it off
//        if (!screenONOnTests && ((PowerManager) getSystemService(Context.POWER_SERVICE)).isInteractive()) {
//            // show alert dialog to wait for screen off
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            alertDialogScreenOff = builder.setTitle("Screen off please").setMessage("Turn screen off").create();
//            alertDialogScreenOff.show();
//
//            // wait for screen off and then start counting
//            runAfterScreenOff(new Runnable() {
//                public void run() {
//                    transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
//                }
//            });
//
//        } else {
        transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
//        }
    }

    /**
     *
     */
    private void runAfterScreenOff(final Runnable screenOffTask) {
        // acquire a partial wake lock, to stay with cpu running when screen off
        wakeLock.acquire();

        // screen off broadcast receiver
        screenOffBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(final Context context, Intent intent) {
                Log.i(TAG, "Screen off");
                unregisterReceiver(screenOffBroadcastReceiver);

                isWakeLockRunning = true;

                // this is an asynchronous task
                screenOffTask.run();
            }
        };

        // register broadcast receiver for screen off
        registerReceiver(screenOffBroadcastReceiver, intentFilterScreenOff);
    }

    /*
     *
     */
    public void doNotification(String id, String text) {
        notify(this, 0, id, text, null, Color.WHITE, 1, 0);
    }

    /*
     *
     */
    public void doNotificationWait() {
        // led flashing
        notify(this, 0, "Test", "Wait...", null, Color.BLUE, 100, 1000);
    }

    /*
     *
     */
    public void doNotificationStart() {
        // led flashing
        notify(this, 0, "Test", "Running test " + numberOfCurrentTest + " of " + numberOfTransmittingTestsToDo,
                notificationSoundUri, Color.RED, 100, 1000);
    }

    /*
     *
     */
    public void doNotificationEnd() {
        // led on
        notify(this, 0, "Test", "Finished...", notificationSoundEndUri, Color.RED, 1, 0);
    }

    /**
     *
     */
    public void notify(Context context, int notificationID, String title, String text, Uri soundUri, int lightArg,
                       int ledOnMs, int ledOffMs) {

        Notification notification = new Notification.Builder(context)
                .setContentTitle(title).setContentText(text).setSmallIcon(R.drawable.icon)
                .setSound(soundUri).setLights(lightArg, ledOnMs, ledOffMs)
                .setPriority(Notification.PRIORITY_HIGH).build();

        notificationManager.notify(notificationID, notification);
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
            btnSendData.setVisibility(View.GONE);
            btnStopTransmitting.setVisibility(View.VISIBLE);
            btnSendImage.setEnabled(false);
        } else {
            btnSendImage.setVisibility(View.GONE);
            btnStopSendingImage.setVisibility(View.VISIBLE);
            btnSendData.setEnabled(false);
        }
        setEnabledTransmissionInputViews(false);
    }

    /**
     * @param sourceUri null Sending dummy data, not null sending a file
     */
    public void endTransmittingGuiActions(Uri sourceUri) {
        if (sourceUri == null) {
            btnSendData.setVisibility(View.VISIBLE);
            btnStopTransmitting.setVisibility(View.GONE);
            if (communicationMode == COMM_MODE.TCP)
                btnSendImage.setEnabled(true);
        } else {
            btnStopSendingImage.setVisibility(View.GONE);
            btnSendImage.setVisibility(View.VISIBLE);
            btnSendData.setEnabled(true);
        }
        setEnabledTransmissionInputViews(true);

        // there is more transmit tests to be done?
        if (++numberOfCurrentTest <= numberOfTransmittingTestsToDo) {
            doNotificationWait();

            // start new execution, after some time
            transmitHandler.postDelayed(transmitTask, delayBeforeEachTransmitTestMS);
        } else {
            //if (isWakeLockRunning) {
            // end of tests, launch end notification and release wakelock
            doNotificationEnd();
            Log.d(TAG, "END OF TEST .................................");
            //wakeLock.release();
            //isWakeLockRunning = false;
            //}
        }

    }

    /**
     *
     */
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
    private void transmitData(Uri fileToSend, String crIpAddress, int crPortNumber, String destIpAddress,
                              int destPortNumber, int bufferSizeKB, long delayMs, long totalBytesToSend,
                              BIND_TO_NETWORK bindToNetwork) {

        try {
            switch (communicationMode) {
                case TCP:
                    clientTransmitter = new ClientSendDataThreadTCP(destIpAddress, destPortNumber
                            , crIpAddress, crPortNumber, delayMs, totalBytesToSend
                            , tvTxThrdSentData, tvTxThrdRcvData, this, bufferSizeKB, fileToSend, bindToNetwork);
                    break;

                case UDP:
                    // build the UDP transmitter socket
                    UDPSocket sock = UDPSocket.getUDPTransmitterSocket(crIpAddress, crPortNumber, bindToNetwork);
                    Network network = null;

                    if (bindToNetwork.equals(BIND_TO_NETWORK.WF)) {
                        network = getNetworkWF();
                    }

                    if (bindToNetwork.equals(BIND_TO_NETWORK.WFD)) {
                        network = getNetworkWFD();
                    }

                    // create worker thread transmitting by UDP socket
                    clientTransmitter = new ClientSendDataThreadUDP(destIpAddress, destPortNumber
                            , sock, delayMs, totalBytesToSend, tvTxThrdSentData, this, bufferSizeKB, network);
                    break;

                case UDP_MULTICAST:
                    // get interface if any
                    NetworkInterface netInterface = rbMulticastNetIntWFD.isChecked() ? wfdNetworkInterface :
                            rbMulticastNetIntWF.isChecked() ? wfNetworkInterface : null;

                    // build he UDP multicast transmitter socket
                    UDPSocket multicastSocket = UDPSocket.getUDPMulticastTransmitterSocket(crIpAddress, crPortNumber,
                            netInterface);

                    // create worker thread transmitting in UDP multicast socket
                    clientTransmitter = new ClientSendDataThreadUDP(destIpAddress, destPortNumber
                            , multicastSocket, delayMs, totalBytesToSend, tvTxThrdSentData, this, bufferSizeKB, null);

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

            doTransmit(uriFileToSend);
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

        stopLogDirSystem();

        wakeLock.release();
        wifiLock.release();

        if (wifiMulticastLock.isHeld()) {
            Log.d(TAG, "Multicast lock released");
            wifiMulticastLock.release();
        }

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
    private void setVisibleReceptionReplyMode(boolean visible) {
        llReceptionReplyMode.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /*
     *
     */
    private void setEnabledTransmissionInputViews(boolean enable) {
        editTextCrIpAddress.setEnabled(enable);
        editTextCrIpAddressLastPart.setEnabled(enable);
        editTextCrPortNumber.setEnabled(enable);
        editTextDestIpAddress.setEnabled(enable);
        editTextDestPortNumber.setEnabled(enable);
        editTextMaxBufferSize.setEnabled(enable);
        editTextDelay.setEnabled(enable);
        editTextTotalBytesToSend.setEnabled(enable);
        btnSentKBytes.setEnabled(enable);
        btnSentMBytes.setEnabled(enable);
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
    public static UDPSocket getUDPTransmitterSocket(String crIpAddress, int crPort,
                                                    ClientActivity.BIND_TO_NETWORK bindToNetwork)
            throws UnknownHostException, SocketException {

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

        Log.d(ClientActivity.TAG, "Multicast transmitter: will use network interface: " +
                (netInterface != null ? netInterface.getName() : null));

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

        Log.d(ClientActivity.TAG, "Multicast receiver: will use network interface: " +
                (netInterface != null ? netInterface.getName() : null));

        if (netInterface != null) {
            // joint the sockets to the multicast group defined by the multicast endpoint using the
            // specified interface
            multicastReceiverSocket.joinGroup(iSock, netInterface);
        }

        // return the UDPSocket wrapper class
        return new UDPSocket(crMulticastIpAddress, crMulticastPort, multicastReceiverSocket);
    }

}

