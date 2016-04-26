package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.SystemInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by ATDR on 18-02-2016
 * .
 */
class NetworkInterfacesDetector {
    private final WifiManager wifiManager;
    private final WifiP2pManager p2pManager;
    private final WifiP2pManager.Channel channel;
    private final ConnectivityManager connectivityManager;
    private final IntentFilter intentFilterForWFD = new IntentFilter();
    private final IntentFilter intentFilterForWF = new IntentFilter();


    Context context;
    private BroadcastReceiver wfdBroadcastReceiver;

    private final WifiP2pManager.GroupInfoListener wfdGroupInfoListener;
    private final WFNetworkInterfaceListener wfNetworkInterfaceListener;
    private BroadcastReceiver wfBroadcastReceiver;

    private InetAddress goAddress;


    /*
     *
     */
    public NetworkInterfacesDetector(Context context, WifiP2pManager.GroupInfoListener wfdGroupInfoListener,
                                     WFNetworkInterfaceListener wfNetworkInterfaceListener) {
        this.context = context;
        this.wfdGroupInfoListener = wfdGroupInfoListener;
        this.wfNetworkInterfaceListener = wfNetworkInterfaceListener;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        p2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(context, context.getMainLooper(), null);

        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        //intentFilterForWF.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilterForWF.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        intentFilterForWFD.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilterForWFD.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    /**
     *
     */
    public InetAddress getGoAddress() {
        return goAddress;
    }

    /*
     *
     */
    protected void onResume() {
        context.registerReceiver(getWFDBroadcastReceiver(), intentFilterForWFD);
        context.registerReceiver(getWFBroadcastReceiver(), intentFilterForWF);
    }

    /*
     *
     */
    protected void onPause() {
        context.unregisterReceiver(getWFDBroadcastReceiver());
        context.unregisterReceiver(getWFBroadcastReceiver());
    }

    /*
     * singleton for WFD broadcast receiver
     */
    private BroadcastReceiver getWFDBroadcastReceiver() {
        if (wfdBroadcastReceiver == null) {
            wfdBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                        Log.d(ClientActivity.TAG, "WFD BDC: WIFI_P2P_CONNECTION_CHANGED");
                        update_P2P_connection_changed(intent);
                    }
//                    if (intent.getAction().equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
//                        Log.d(ClientActivity.TAG, "WFD BDC: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
//                        update_P2P_this_device_changed(intent);
//                    }
                }
            };
        }
        return wfdBroadcastReceiver;
    }

    /*
     *
     */
//    private void update_P2P_this_device_changed(Intent intent) {
//        WifiP2pDevice dev = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
//        Log.d(ClientActivity.TAG, "WFD BDC: WifiP2pDevice:  " + dev.toString());
//    }

    /*
     * singleton for WF broadcast receiver
     */
    private BroadcastReceiver getWFBroadcastReceiver() {
        if (wfBroadcastReceiver == null) {
            wfBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    WifiInfo wfInfo = wifiManager.getConnectionInfo();
                    Log.d(ClientActivity.TAG,
                            "WF BDC: ACTION: " + intent.getAction() + " " + wfInfo + " " + wfInfo.getSSID());
                    NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    Log.d(ClientActivity.TAG,
                            "WF BDC: WiFI " + (mWifi.isConnected() ? "connected to: " + wfInfo.getSSID() : "disconnected"));
                    updateNetworkInterfaces();
                }
            };
        }
        return wfBroadcastReceiver;
    }

    private String getWifiStateString(int wifiState) {
        String[] state = {"WIFI_STATE_DISABLING", "WIFI_STATE_DISABLED", "WIFI_STATE_ENABLING", "WIFI_STATE_ENABLED", "WIFI_STATE_UNKNOWN"};
        if (wifiState < 0 && wifiState > 4)
            throw new IllegalArgumentException("Invalid wifiState value: " + wifiState);
        return state[wifiState];
    }

    /*
     *
     */
    public interface WFNetworkInterfaceListener {
        void updateWFNetworkInterface(NetworkInterface wfInterface);
    }

    /*
     *
     */
    public void updateNetworkInterfaces() {
        ArrayList<NetworkInterface> netInts = new ArrayList<>();
        NetworkInterface wfd = null, wf = null;


        try {
            Log.d(ClientActivity.TAG, "List of network interfaces:");
            for (NetworkInterface netInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                Log.d("NETINT", netInterface.toString() + " " + SystemInfo.getMACStringFromBytes(netInterface.getHardwareAddress()));
                // 3G networks don't support broadcast
                if (!netInterface.isLoopback() && netInterface.isUp() && Collections.list(
                        netInterface.getInetAddresses()).size() == 2 &&
                        ClientActivity.getIPV4AddressWithBroadcast(netInterface) != null) {
                    Log.d(ClientActivity.TAG, "> " + netInterface + " inetAdrrs " + Collections.list(
                            netInterface.getInetAddresses()).size() + " " + netInterface.supportsMulticast());

                    if (wf == null && netInterface.getName().startsWith("wlan")) {
                        wf = netInterface;
                    } else if (wfd == null && netInterface.getName().startsWith("p2p")) {
                        wfd = netInterface;
                    } else
                        netInts.add(netInterface);
                }
            }

            // try to get the last network interface, just in case of a different name
            if (wf == null && wfd != null && netInts.size() == 1)
                wf = netInts.get(0);

            if (wfd == null && wf != null && netInts.size() == 1)
                wfd = netInts.get(0);

            Log.d(ClientActivity.TAG, "FINAL WFD: " + wfd + ", WF: " + wf);

            // tvWFDState.setText("WFD:  " + (wfd == null ? "OFF/NC" : wfd.getName() + "  " +
            //        getIPV4AddressWithBroadcast(wfd).toString().substring(1)));

            // call WF listener to make GUI update
            wfNetworkInterfaceListener.updateWFNetworkInterface(wf);

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /*
     *
     */
    private void update_P2P_connection_changed(Intent intent) {
        WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        if (!wifiP2pInfo.groupFormed) {
            // call groupInfoAvailable with null on group
            wfdGroupInfoListener.onGroupInfoAvailable(null);
            goAddress = null;
            return;
        }

        goAddress = wifiP2pInfo.groupOwnerAddress;

        // get wifiP2pGroup in asynchronous way for API 16
        p2pManager.requestGroupInfo(channel, wfdGroupInfoListener);
    }

}
