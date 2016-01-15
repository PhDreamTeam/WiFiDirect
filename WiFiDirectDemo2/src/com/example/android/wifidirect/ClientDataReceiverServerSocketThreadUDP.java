package com.example.android.wifidirect;

import android.util.Log;
import android.widget.LinearLayout;
import com.example.android.wifidirect.utils.LoggerSession;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by DR e AT on 27/05/2015.
 * .
 */
public class ClientDataReceiverServerSocketThreadUDP extends Thread implements IStoppable {
    private int bufferSizeKB;
    int portNumber;
    DatagramSocket serverDatagramSocket;
    boolean run = true;
    LinearLayout llReceptionZone;
    byte buffer[];
    long nBytesReceived, rcvDataCounterLastValue;
    long lastUpdate;
    private ReceptionGuiInfo receptionGuiInfoGui;
    double maxSpeedMbps;

    ArrayList<DataTransferInfo> transferInfoArrayList = new ArrayList<>();

    public ClientDataReceiverServerSocketThreadUDP(int portNumber,
                                                   LinearLayout llReceptionZone, int bufferSizeKB) {
        this.llReceptionZone = llReceptionZone;
        this.portNumber = portNumber;
        this.bufferSizeKB = bufferSizeKB;
        buffer = new byte[bufferSizeKB];
    }


    @Override
    public void run() {
        try {
            serverDatagramSocket = new DatagramSocket(portNumber);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // to catch different transmissions
            while (run) {

                int nBuffersReceived = 0;
                long initialTimeMs = 0;
                maxSpeedMbps = lastUpdate = 0;
                nBytesReceived = rcvDataCounterLastValue = 0;

                LoggerSession logSession = null;
                byte bufferRcv[];
                ByteBuffer bufferInt;

                // to process successive datagrams from one transmission
                // it is supposed that transmissions do not overlap
                while (run) {
                    // wait connections
                    serverDatagramSocket.receive(packet);

                    if (++nBuffersReceived == 1) {
                        // start counting for gui update
                        lastUpdate = System.nanoTime();
                        // create gui session
                        receptionGuiInfoGui = new ReceptionGuiInfo(llReceptionZone, packet.getAddress().toString() +
                                ":" + packet.getPort(), portNumber, transferInfoArrayList, null);

                        // start log session and log initial time
                        logSession = MyMainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName() +
                                " Receiving UDP data ");
                        initialTimeMs = logSession.logTime("Initial time");
                        logSession.logMsg("Sender address: " + packet.getAddress().toString() + ":" + packet.getPort());
                        Log.d("FWD Receiver UDP", "Received a datagram from: " +
                                packet.getAddress().toString());
                    }

                    bufferRcv = packet.getData();
                    // extract datagram number
                    bufferInt = ByteBuffer.wrap(bufferRcv, 4, 4);
                    int datagramNumber = bufferInt.getInt();
                    // logSession.logMsg("Received buffer: " + datagramNumber);
                    // Log.d("FWD Receiver UDP", "Received datagram number: " + datagramNumber +
                    //        " from " + packet.getAddress().toString());

                    nBytesReceived += bufferRcv.length;

                    updateVisualDeltaInformation(false);

                    // termination condition
                    if (datagramNumber == 0) {
                        Log.d("FWD Receiver UDP", "Ended reception from: " + packet.getAddress().toString());
                        break;
                    }
                }

                updateVisualDeltaInformation(true);

                long finalTimeMs = logSession.logTime("Final time");
                double deltaTimeSegs = (finalTimeMs - initialTimeMs) / 1000.0;
                double receivedDataMb = ((double) (nBytesReceived * 8)) / (1024 * 1204);
                double globalRcvSpeedMbps = receivedDataMb / deltaTimeSegs;

                logSession.logMsg("Received " + nBuffersReceived + " buffers");
                logSession.logMsg("Received bytes: " + nBytesReceived);
                logSession.logMsg("Receive global speed (Mbps): " + String.format("%5.3f", globalRcvSpeedMbps));

                logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
                for (DataTransferInfo data : transferInfoArrayList)
                    logSession.logMsg("  " + data);
                logSession.close();

                transferInfoArrayList.clear();

                receptionGuiInfoGui.setTerminatedState();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *
     */
    void updateVisualDeltaInformation(boolean forcedUpdate) {
        // elapsed time
        long currentNanoTime = System.nanoTime();

        if (currentNanoTime > lastUpdate + 1_000_000_000 || forcedUpdate) {

            long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
            double elapsedDeltaRcvTimeSeconds = elapsedDeltaRcvTimeNano / 1_000_000_000.0;
            long deltaReceivedBytes = nBytesReceived - rcvDataCounterLastValue;
            double speedMbps = ((deltaReceivedBytes * 8) / (1024.0 * 1024)) / elapsedDeltaRcvTimeSeconds;
            lastUpdate = currentNanoTime;
            rcvDataCounterLastValue = nBytesReceived;

            if (speedMbps > maxSpeedMbps)
                maxSpeedMbps = speedMbps;

            receptionGuiInfoGui.setData(nBytesReceived / 1024.0, 0, maxSpeedMbps, speedMbps);

            transferInfoArrayList.add(new DataTransferInfo(speedMbps, elapsedDeltaRcvTimeSeconds,
                    deltaReceivedBytes, currentNanoTime));
        }
    }

    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
    }
}
