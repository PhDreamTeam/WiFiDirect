package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.util.Log;
import android.widget.LinearLayout;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

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
        logSession = MainActivity.logger.getNewLoggerSession("ClientDataReceiverServerSocketThreadUDP " +
                this.getClass().getSimpleName() + " Receiving UDP data ", clientActivity.getLogDir());
        logSession.logMsg("Sender address: " + senderIpAddress + ":" + senderPortNumber);
        Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG,
                "Started a reception from: " + senderIpAddress + ":" + senderPortNumber);

        // log initial time
        logSession.logTime("Initial time");
        logSession.startLoggingBatteryValues(clientActivity);
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
                logSession.logMsg("Destination: " + dataStr[0] + ":" + dataStr[1]);

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
        logSession.stopLoggingBatteryValues();

        Log.d(ClientDataReceiverServerSocketThreadUDP.LOG_TAG, "Ended reception from: " +
                senderIpAddress + ":" + senderPortNumber);

        double deltaTimeSegs = (finalTimeNs - initialTimeNs) / 1_000_000_000.0;
        double receivedDataMb = (nBytesReceived * 8.0) / (1024.0 * 1024.0);
        double globalRcvSpeedMbps = receivedDataMb / deltaTimeSegs;

        // write final global average speed
        receptionGuiInfoGui.setCurAvgRcvSpeed(globalRcvSpeedMbps);

        // log data
        logSession.logMsg("\r\nReceived bytes: " + nBytesReceived);
        logSession.logMsg("Received: " + nBuffersReceived + " buffers of " + (bufferSizeBytes / 1024) + "KBs");
        logSession.logMsg("Receive global speed (Mbps): " + String.format(Locale.US, "%5.3f", globalRcvSpeedMbps));
        logSession.logMsg("Receive max speed (Mbps): " + String.format(Locale.US, "%5.3f", maxSpeedMbps));

        // get average Speed without first and last results
        double totalRegisteredSpeedMbps = 0;
        int numSpeedRegisters = transferInfoArrayList.size();
        for (int i = 1; i < numSpeedRegisters - 1; ++i) {
            DataTransferInfo data = transferInfoArrayList.get(i);
            totalRegisteredSpeedMbps += data.speedMbps;
        }
        logSession.logMsg("Receive avg speed (excluding limits, Mbps): " +
                String.format(Locale.US, "%5.3f", totalRegisteredSpeedMbps / (numSpeedRegisters - 2)) + "\r\n");

        logSession.logBatteryConsumedJoules();
        logSession.logMsg("");

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
                    "Bytes received: " + String.format(Locale.US, "%.1f", bytesReceivedPercentage * 100) + "%");
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
        logSession.logMsg("Receive max speed (Mbps): " + String.format(Locale.US, "%5.3f", maxSpeedMbps));

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
