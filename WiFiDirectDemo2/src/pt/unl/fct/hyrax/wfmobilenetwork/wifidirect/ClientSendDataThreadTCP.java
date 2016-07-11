package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Network;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import javax.net.SocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * Created by DR & AT on 20/05/2015.
 * .
 */
public class ClientSendDataThreadTCP extends Thread implements IStoppable {
    public static final String TAG = ClientActivity.TAG + " TCP";

    private int bufferSize;
    String destIpAddress;
    int destPortNumber;
    String crIpAddress;
    int crPortNumber;
    long speed = 0; // number of millis to sleep between each 4096 of sent Bytes
    long nBytesToSend = 0;
    long rcvData = 0;
    long lastUpdate;
    Uri sourceUri;

    TextView tvSentData;
    TextView tvRcvData;
    ClientActivity clientActivity;

    Thread rcvThread;
    Socket cliSocket = null;
    private double nextNotificationValue = 0.1f;
    private ClientActivity.BIND_TO_NETWORK bindToNetwork;
    private long txDataCounterLastValue;
    private double maxDataSendSpeedMbps;

    /*
     *
     */
    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber
            , long speed, long dataLimitKB, TextView tvSentData, TextView tvRcvData, ClientActivity clientActivity
            , int bufferSize, Uri sourceUri, ClientActivity.BIND_TO_NETWORK bindToNetwork) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.speed = speed;
        this.nBytesToSend = dataLimitKB * 1024;
        this.tvSentData = tvSentData;
        this.tvRcvData = tvRcvData;
        this.clientActivity = clientActivity;
        this.bufferSize = bufferSize;
        this.sourceUri = sourceUri;
        this.bindToNetwork = bindToNetwork;
    }

    static NetworkInterface wifiInterface = null;

    /*
     *
     */
    static NetworkInterface getWLan0NetworkInterface() {
        if (wifiInterface == null) {
//            try {
//                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//
//                for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
//                    if (networkInterface.getDisplayName().equals("wlan0")) {
//                        // || networkInterface.getDisplayName().equals("eth0")) {
//                        wifiInterface = networkInterface;
//                        break;
//                    }
//                }
//            } catch (SocketException e) {
//                e.printStackTrace();
//            }

            try {
                wifiInterface = NetworkInterface.getByName("wlan0");
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return wifiInterface;
    }

    /**
     * This method gets the InetAddress for this received address (trying to) using the wlan0 interface.
     * This is a test, to verify if we can send a message throught one speicify interface
     */
    public static InetAddress getInetAddress(String destAddress) {
        try {
            // AT this code is working in ISEL - IPv6 experiments
            // InetAddress dest1 = Inet6Address.getByName(destAddress);
            // Inet6Address dest = Inet6Address.getByAddress(destAddress, dest1.getAddress(), getWLan0NetworkInterface());

            // AT this code worked in ISEL (with the ipv6 address)
            // InetAddress dest = Inet6Address.getByName(destAddress);

            // to work with ipv4 and ipv6
            return InetAddress.getByName(destAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     *
     */
    @Override
    public void run() {
        // Send data buffer, filled with numbers if not file to be transmitted
        byte buffer[] = new byte[bufferSize];
        if (sourceUri == null) {
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (byte) i;
            }
        }

        ContentResolver cr = null;
        InputStream is = null;
        DataOutputStream dos = null;

        long fileSize = 0, sentData = 0;
        String fileName = null;

        LoggerSession logSession = null;

        try {
            if (bindToNetwork.equals(ClientActivity.BIND_TO_NETWORK.WF)) {
                // bind to WF network
                Network networkWifi = clientActivity.getNetworkWF();
                Log.d(TAG, "Bind socket to network: WF " + networkWifi);
                SocketFactory sf = networkWifi.getSocketFactory();
                cliSocket = sf.createSocket(getInetAddress(crIpAddress), crPortNumber);

            } else if (bindToNetwork.equals(ClientActivity.BIND_TO_NETWORK.WFD)) {
                // bind to WFD network - get P2P network not working

                Network networkWifi = clientActivity.getNetworkWFD();
                Log.d(TAG, "Bind socket to network: WFD " + networkWifi);
                SocketFactory sf = networkWifi.getSocketFactory();
                cliSocket = sf.createSocket(getInetAddress(crIpAddress), crPortNumber);

            } else {
                // InetAddress localIpAddress = InetAddress.getByName("192.168.49.241");
                cliSocket = new Socket(getInetAddress(crIpAddress), crPortNumber);
            }

            dos = new DataOutputStream(cliSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(cliSocket.getInputStream());

            String msg = "Start transmission to CR " + crIpAddress + ":" + crPortNumber + " with dest " +
                    destIpAddress + ":" + destPortNumber;
            //AndroidUtils.toast(tvSentData, msg);
            Log.d(TAG, msg);

            // start log session and log initial time
            logSession = MainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName(),
                    clientActivity.getLogDir());
            logSession.logMsg("Send data to CR: " + crIpAddress + ":" + crPortNumber);
            logSession.logMsg("Send data to dest: " + destIpAddress + ":" + destPortNumber + "\r\n");

            long initialTxTimeMs = logSession.logTime("Initial time");
            logSession.startLoggingBatteryValues(clientActivity);

            if (sourceUri != null) {
                cr = tvSentData.getContext().getContentResolver();
                is = cr.openInputStream(sourceUri);
                // get file size
                fileSize = cr.openTypedAssetFileDescriptor(sourceUri, "*/*", null).getLength();
                fileName = getFileNameFromURI(sourceUri);

                // Log.d(WiFiDirectActivity.TAG, "File URI: " + sourceUri.toString());
                Log.d(TAG, "Sending file: " + fileName + " with length (B): " + fileSize +
                        ", with BufferSize (B): " + buffer.length);
                nBytesToSend = 0; // send the complete image
            } else
                Log.d(TAG, "Sending data (B): " + nBytesToSend + ", with BufferSize (B): " + buffer.length);


            // receive replies from destination
            rcvThread = createRcvThread(dis);

            // send destination information for the forward node
            String addressData = this.destIpAddress + ";" + this.destPortNumber;
            if (sourceUri != null) {
                addressData += ";" + fileName + ";" + fileSize;
            }
            dos.writeInt(addressData.getBytes().length);
            dos.write(addressData.getBytes());
            dos.writeLong(nBytesToSend);

            int dataLen = buffer.length;
            int nBuffersSent = 0;

            while (true) {
                if (is != null) {
                    dataLen = is.read(buffer);
                    if (dataLen == -1) {
                        break;
                    }
                }

                dos.write(buffer, 0, dataLen);
                sentData += dataLen;
                updateSentData(sentData, false);
                Log.v(TAG, "Sent buffer nº " + ++nBuffersSent + ", with bytes: " + dataLen);

                if (nBytesToSend != 0 && sentData >= nBytesToSend) {
                    break;
                }
                if (speed != 0) {
                    Thread.sleep(speed);
                }
            }

            updateSentData(sentData, true);
            Log.d(TAG, "EOT, data sent: " + sentData);

            cliSocket.shutdownOutput();

            // log end writing time
            long finalTxTimeMs = logSession.logTime("Final sent time");
            logSession.stopLoggingBatteryValues();

            // wait for received thread to terminate and log time
            rcvThread.join();
            logSession.logTime("Final receive time");
            double deltaTimeSegs = (finalTxTimeMs - initialTxTimeMs) / 1000.0;
            logSession.logMsg("Time elapsed (s): " + String.format("%5.3f", deltaTimeSegs) + "\r\n");

            // log final sent and receive bytes
            logSession.logMsg("Data sent (B): " + sentData + ", (MB): " + sentData / (1024.0 * 1024));
            double sentDataMb = ((double) (sentData * 8)) / (1024 * 1024);
            double dataSentSpeedMbps = sentDataMb / deltaTimeSegs;
            logSession.logMsg("Data sent speed (Mbps): " + String.format("%5.3f", dataSentSpeedMbps));
            logSession.logMsg("Data sent Max speed (Mbps): " + String.format("%5.3f", maxDataSendSpeedMbps));

            logSession.logMsg("Data received (B): " + rcvData + ", (MB): " + rcvData / (1024.0 * 1024) + "\r\n");

            logSession.logBatteryConsumedJoules();

            logSession.close(tvSentData.getContext());

        } catch (Exception e) {
            String msg = "Transmission stopped, cause: " +
                    (e.getMessage().equals("Socket closed") ? "by user action" : e.getMessage());
            AndroidUtils.toast(tvSentData, msg);
            Log.e(TAG, msg);
            e.printStackTrace();
            // e.printStackTrace();
            if (logSession != null) {
                logSession.logMsg(msg);
                logSession.close(tvSentData.getContext());
            }
        } finally {
            // close streams
            AndroidUtils.close(is);
            // close(dos);
        }

        tvSentData.post(new Runnable() {
            @Override
            public void run() {
                clientActivity.endTransmittingGuiActions(sourceUri);
            }
        });
    }


    /*
     *
     */
    private String getFileNameFromURI(Uri returnUri) {
        Cursor returnCursor =
                tvSentData.getContext().getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        String fileName = returnCursor.getString(nameIndex);
        //sizeView.setText(Long.toString(returnCursor.getLong(sizeIndex)));

        returnCursor.close();

        return fileName;
    }

    /*
     *
     */
    private Thread createRcvThread(final DataInputStream dis) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte buffer[] = new byte[bufferSize];

                int nBytesRead = 0;

                try {
                    while (true) {
                        nBytesRead = dis.read(buffer);
                        if (nBytesRead == -1)
                            break;

                        rcvData += nBytesRead;
                    }
                    updateRcvData();

                } catch (IOException e) {
                    String msg = "Socket receiver part stopped, cause: " +
                            (e.getMessage().equals("Socket closed") ? "by user action" : e.getMessage());
                    Log.d(TAG, msg);
                    // e.printStackTrace();
                } finally {
                    AndroidUtils.close(dis);
                }
            }
        });
        thread.start();
        return thread;
    }

    /*
     *
     *
     */
    private void updateSentData(final long sentData, boolean forceUpdate) {
        long currentNanoTime = System.nanoTime();

        if ((currentNanoTime > lastUpdate + 1_000_000_000) || forceUpdate) {
            long elapsedDeltaTxTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
            double elapsedDeltaTxTimeSeconds = elapsedDeltaTxTimeNano / 1_000_000_000.0;
            long deltaTxBytes = sentData - txDataCounterLastValue;
            final double speedMbps = ((deltaTxBytes * 8) / (1024.0 * 1024)) / elapsedDeltaTxTimeSeconds;

            // exclude last reading
            if (!forceUpdate && speedMbps > maxDataSendSpeedMbps)
                maxDataSendSpeedMbps = speedMbps;

            lastUpdate = currentNanoTime;
            tvSentData.post(new Runnable() {
                @Override
                public void run() {
                    tvSentData.setText("" + (sentData / 1024));
                }
            });
            updateRcvData();

            txDataCounterLastValue = sentData;
        }

        // ADB console notification
        float bytesSentPercentage = sentData / (float) nBytesToSend;
        if (bytesSentPercentage > nextNotificationValue) {
            Log.d(TAG, "Bytes sent: " + String.format("%.1f", bytesSentPercentage * 100) + "%");
            nextNotificationValue += 0.1f;
        }
    }

    /*
     *
     */
    private void updateRcvData() {
        tvRcvData.post(new Runnable() {
            @Override
            public void run() {
                tvRcvData.setText("" + rcvData);
            }
        });
    }

    /*
     *
     */
    @Override
    public void stopThread() {
        //this.interrupt();
        //rcvThread.interrupt();

        try {
            if (cliSocket != null)
                cliSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
