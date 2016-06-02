package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.util.Log;
import android.widget.LinearLayout;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by DR e AT on 27/05/2015.
 * .
 */
public class ClientDataReceiverServerSocketThreadUDP extends Thread implements IStoppable {
    static final String LOG_TAG = "FWD Receiver UDP";

    private ArrayList<UDPSession> udpSessions = new ArrayList<>();
    private ClientActivity clientActivity;
    private DatagramSocket serverDatagramSocket;
    private LinearLayout llReceptionZone;
    private int localPortNumber;
    private int bufferSizeBytes;
    private byte buffer[];


    /**
     *
     */
    public ClientDataReceiverServerSocketThreadUDP(UDPSocket serverSocket, LinearLayout llReceptionZone,
                                                   int bufferSizeBytes, ClientActivity clientActivity) {
        this.llReceptionZone = llReceptionZone;
        this.clientActivity = clientActivity;
        this.bufferSizeBytes = bufferSizeBytes;

        serverDatagramSocket = serverSocket.getSocket();
        localPortNumber = serverSocket.getCrPort();

        buffer = new byte[bufferSizeBytes];
    }

    /**
     * Do datagram package distribution by the several occurring transmissions
     */
    @Override
    public void run() {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            Log.d(LOG_TAG, "Starting UDP reception server at port: " + localPortNumber);

            while (true) {
                // wait for datagrams
                serverDatagramSocket.receive(packet);

                String senderIpAddress = packet.getAddress().toString().substring(1);

                // check for end of reception
                if (senderIpAddress.equals("127.0.0.1")) {

                    // end of receiving actions, called by user
                    AndroidUtils.toast(llReceptionZone, "Reception stopped");
                    // close datagram socket
                    serverDatagramSocket.close();

                    // signal current transmissions to finish
                    for (UDPSession session : udpSessions) {
                        if (!session.isFinishedTransmission())
                            session.finishTransmissionByLocalUser();
                    }

                    Log.d(LOG_TAG, "Stopping reception from GUI action");

                    // stop receiving cycle
                    break;
                }

                // get the UDP sessions (or a new one) for this sender (ip & port) and process packet on it
                UDPSession session = getUdpSession(senderIpAddress, packet.getPort());
                session.processUDPPackage(packet);
            }

        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toast(llReceptionZone, "Exception " + e.getMessage());
        }

        // do end of receiving gui actions on client activity
        llReceptionZone.post(new Runnable() {
            @Override
            public void run() {
                clientActivity.endReceivingGuiActions();
            }
        });
    }

    /**
     *
     */
    private UDPSession getUdpSession(String senderIpAddress, int senderPort) {
        // look for an existing session, if one have the same sender properties return it
        for (UDPSession s : udpSessions) {
            if (s.haveSameSender(senderIpAddress, senderPort))
                return s;
        }

        // no existing session found, create a new one, add with to current sessions and return it
        UDPSession session = new UDPSession(senderIpAddress, senderPort, localPortNumber, clientActivity,
                llReceptionZone, bufferSizeBytes);
        udpSessions.add(session);

        return session;
    }


    /**
     *
     */
    @Override
    public void stopThread() {
        // stop socket listening - send a termination: a dummy local datagram
        // must run in the non GUI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket ds = new DatagramSocket();
                    ds.send(new DatagramPacket(new byte[0], 0,
                            InetAddress.getByName("127.0.0.1"), localPortNumber));
                    ds.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

/**
 * A UDP transmission
 */
class UDPSession {
    private ArrayList<DataTransferInfo> transferInfoArrayList = new ArrayList<>();
    private final LinearLayout llReceptionZone;
    private int nBuffersReceived;
    private long initialTimeNs;
    private long lastUpdate;
    private int rcvDataCounterLastValue;
    private int nBytesReceived;
    private double maxSpeedMbps;
    private ReceptionGuiInfo receptionGuiInfoGui;
    private ClientActivity clientActivity;
    private LoggerSession logSession;
    private String senderIpAddress;
    private int senderPortNumber;
    private int bufferSizeBytes;
    private boolean isFinishedTransmission = false;
    private int nBuffersToBeReceived;
    private double nextNotificationValue = 0.1f;


    /**
     *
     */
    UDPSession(String senderIpAddress, int senderPortNumber, int localPortNumber, ClientActivity clientActivity,
               LinearLayout llReceptionZone, int bufferSizeBytes) {
        this.clientActivity = clientActivity;
        this.senderIpAddress = senderIpAddress;
        this.senderPortNumber = senderPortNumber;
        this.llReceptionZone = llReceptionZone;
        this.bufferSizeBytes = bufferSizeBytes;

        // create gui session
        receptionGuiInfoGui = new ReceptionGuiInfo("UDP", llReceptionZone,
                senderIpAddress + ":" + senderPortNumber, localPortNumber, transferInfoArrayList, null);

        clientActivity.registerReceptionGuiInfo(receptionGuiInfoGui);

        // init transmission data
        initialTimeNs = lastUpdate = System.nanoTime();
        rcvDataCounterLastValue = 0;
        nBuffersReceived = 0;
        nBytesReceived = 0;
        maxSpeedMbps = 0;

        // start log session
        logSession = MainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName() +
                " Receiving UDP data ", clientActivity.getLogDir());
        logSession.logMsg("Sender address: " + senderIpAddress + ":" + senderPortNumber);
        Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG,
                "Started a reception from: " + senderIpAddress + ":" + senderPortNumber);

        // log initial time
        logSession.logTime("Initial time");
    }

    /**
     * first 4 bytes: string with content; second 4 bytes: sequence number (decedent), last is 0
     * first 4  bytes with -1 means that transmission is already finished by the transmitter
     */
    void processUDPPackage(DatagramPacket packet) {
        if (isFinishedTransmission())
            return;

        byte[] bufferRcv = packet.getData();

        int dimContentString = ByteBuffer.wrap(bufferRcv, 0, 4).getInt();

        if (dimContentString != -1) {

            if (++nBuffersReceived == 1) {
                String addressInfo = new String(bufferRcv, 8, dimContentString);
                String dataStr[] = addressInfo.split(";");
                logSession.logMsg("Destination: " + dataStr[0] + ":" + dataStr[1] + "\r\n");

                nBuffersToBeReceived = ByteBuffer.wrap(bufferRcv, 8 + dimContentString, 4).getInt();
                Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG,
                        "Should receive " + nBuffersToBeReceived + " buffers");
            }

            // extract datagram number
            ByteBuffer bufferInt = ByteBuffer.wrap(bufferRcv, 4, 4);
            int datagramNumber = bufferInt.getInt();

            // logSession.logMsg("Received buffer: " + datagramNumber);
            // Log.d("FWD Receiver UDP", "Received datagram number: " + datagramNumber +
            //        " from " + packet.getAddress().toString());

            nBytesReceived += bufferRcv.length;

            if (datagramNumber != 0) {
                // transmission did not finished
                updateVisualDeltaInformation(false);
                return;
            }
        } else {
            // end of transmission but already lost final packet
            Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG, "INVALID SESSION: Received termination packet from: " +
                    senderIpAddress + ":" + senderPortNumber);
            logSession.logMsg("INVALID SESSION: Received termination packet from: " +
                    senderIpAddress + ":" + senderPortNumber);
        }

        // end of transmission actions

        updateVisualDeltaInformation(true);

        long finalTimeNs = lastUpdate;
        logSession.logTime("Final time");
        logSession.logMsg("");

        Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG, "Ended reception from: " +
                senderIpAddress + ":" + senderPortNumber);

        double deltaTimeSegs = (finalTimeNs - initialTimeNs) / 1_000_000_000.0;
        double receivedDataMb = (nBytesReceived * 8.0) / (1024.0 * 1024.0);
        double globalRcvSpeedMbps = receivedDataMb / deltaTimeSegs;

        // write final global average speed
        receptionGuiInfoGui.setCurAvgRcvSpeed(globalRcvSpeedMbps);

        // log data
        logSession.logMsg("Received bytes: " + nBytesReceived);
        logSession.logMsg("Received: " + nBuffersReceived + " buffers of " + (bufferSizeBytes / 1024) + "KBs");
        logSession.logMsg("Receive global speed (Mbps): " + String.format("%5.3f", globalRcvSpeedMbps));
        logSession.logMsg("Receive max speed (Mbps): " + String.format("%5.3f", maxSpeedMbps));

        // get average Speed without first and last results
        double totalRegisteredSpeedMbps = 0;
        int numSpeedRegisters = transferInfoArrayList.size();
        for (int i = 1; i < numSpeedRegisters - 1; ++i) {
            DataTransferInfo data = transferInfoArrayList.get(i);
            totalRegisteredSpeedMbps += data.speedMbps;
        }
        logSession.logMsg("Receive avg speed (excluding limits, Mbps): " +
                String.format("%5.3f", totalRegisteredSpeedMbps / (numSpeedRegisters - 2)) + "\n");

        logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
        for (DataTransferInfo data : transferInfoArrayList)
            logSession.logMsg("  " + data);
        logSession.close(llReceptionZone.getContext());

        isFinishedTransmission = true;

        //transferInfoArrayList.clear();

        receptionGuiInfoGui.setTerminatedState();
    }

     /*
     *
     */

    void updateVisualDeltaInformation(boolean forcedUpdate) {
        // elapsed time
        long currentNanoTime = System.nanoTime();

        if (currentNanoTime > lastUpdate + 1_000_000_000 || forcedUpdate) {

            // logSession.logMsg("CurrentNanoTime: " + currentNanoTime);
            // logSession.logMsg("LastUpdate: " + lastUpdate);

            long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 to get seconds
            double elapsedDeltaRcvTimeSeconds = elapsedDeltaRcvTimeNano / 1_000_000_000.0;
            long deltaReceivedBytes = nBytesReceived - rcvDataCounterLastValue;
            double speedMbps = ((deltaReceivedBytes * 8.0) / (1024.0 * 1024.0)) / elapsedDeltaRcvTimeSeconds;
            lastUpdate = currentNanoTime;
            rcvDataCounterLastValue = nBytesReceived;

            // exclude last reading
            if (!forcedUpdate && speedMbps > maxSpeedMbps)
                maxSpeedMbps = speedMbps;

            receptionGuiInfoGui.setData(nBytesReceived / 1024.0, 0, maxSpeedMbps, speedMbps);

            transferInfoArrayList.add(new DataTransferInfo(speedMbps, elapsedDeltaRcvTimeSeconds,
                    deltaReceivedBytes, currentNanoTime));

            // logSession.logMsg("DeltaBytes: " + deltaReceivedBytes);
            // logSession.logMsg("CurrentSpeed (Mbps): " + speedMbps);

        }


        float bytesReceivedPercentage = nBytesReceived / ((float) nBuffersToBeReceived * bufferSizeBytes);
        if (bytesReceivedPercentage > nextNotificationValue) {
            Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG,
                    "Bytes received: " + String.format("%.1f", bytesReceivedPercentage * 100) + "%");
            nextNotificationValue += 0.1f;
        }
    }

    /**
     *
     */
    void finishTransmissionByLocalUser() {
        logSession.logMsg("Transmission stopped by user - GUI");
        logSession.logMsg("Received bytes: " + nBytesReceived);
        logSession.logMsg("Received: " + nBuffersReceived + " buffers of " + (bufferSizeBytes / 1024) + "KBs");
        logSession.logMsg("Receive max speed (Mbps): " + String.format("%5.3f", maxSpeedMbps));

        logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
        for (DataTransferInfo data : transferInfoArrayList)
            logSession.logMsg("  " + data);
        logSession.close(llReceptionZone.getContext());

        isFinishedTransmission = true;
        receptionGuiInfoGui.setTerminatedState();
    }

    /**
     *
     */
    public boolean isFinishedTransmission() {
        return isFinishedTransmission;
    }

    /**
     *
     */
    public boolean haveSameSender(String ipAddress, int portNumber) {
        return senderPortNumber == portNumber && senderIpAddress.equals(ipAddress);
    }
}
