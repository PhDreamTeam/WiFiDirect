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
    private int bufferSizeKB;
    int portNumber;
    UDPSocket udpServerSocket;
    DatagramSocket serverDatagramSocket;
    LinearLayout llReceptionZone;
    byte buffer[];
    long nBytesReceived, rcvDataCounterLastValue;
    long lastUpdate;
    private ReceptionGuiInfo receptionGuiInfoGui;
    double maxSpeedMbps;
    ClientActivity clientActivity;

    ArrayList<DataTransferInfo> transferInfoArrayList = new ArrayList<>();

    public ClientDataReceiverServerSocketThreadUDP(UDPSocket serverSocket, LinearLayout llReceptionZone,
                                                   int bufferSizeKB, ClientActivity clientActivity) {
        this.udpServerSocket = serverSocket;
        this.llReceptionZone = llReceptionZone;
        this.bufferSizeKB = bufferSizeKB;
        this.clientActivity = clientActivity;

        serverDatagramSocket = serverSocket.getSocket();
        portNumber = serverSocket.getCrPort();
        buffer = new byte[bufferSizeKB];
    }


    @Override
    public void run() {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // to catch different transmissions
            while (true) {

                int nBuffersReceived = 0;
                long initialTimeNs = 0;
                maxSpeedMbps = lastUpdate = 0;
                nBytesReceived = rcvDataCounterLastValue = 0;

                LoggerSession logSession = null;
                byte bufferRcv[];
                ByteBuffer bufferInt;

                // to process successive datagrams from one transmission
                // it is supposed that transmissions do not overlap
                while (true) {
                    // wait connections
                    serverDatagramSocket.receive(packet);

                    if (packet.getAddress().toString().equals("/127.0.0.1")) {
                        AndroidUtils.toast(llReceptionZone, "Reception stopped");
                        // close datagram socket
                        serverDatagramSocket.close();
                        if (logSession != null) {
                            logSession.logMsg("Transmission stopped by user - GUI");
                            logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
                            for (DataTransferInfo data : transferInfoArrayList)
                                logSession.logMsg("  " + data);
                            logSession.close(llReceptionZone.getContext());
                        }
                        return;
                    }

                    if (++nBuffersReceived == 1) {
                        // create gui session
                        receptionGuiInfoGui = new ReceptionGuiInfo("UDP", llReceptionZone,
                                packet.getAddress().toString() +
                                        ":" + packet.getPort(), portNumber, transferInfoArrayList, null);

                        // start counting for gui update
                        lastUpdate = System.nanoTime();
                        initialTimeNs = lastUpdate;

                        // start log session and log initial time
                        logSession = MainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName() +
                                " Receiving UDP data ", clientActivity.getLogDir());
                        logSession.logMsg("Sender address: " + packet.getAddress().toString().substring(1) +
                                ":" + packet.getPort());
                        Log.d("FWD Receiver UDP", "Received a datagram from: " +
                                packet.getAddress().toString().substring(1));
                    }

                    bufferRcv = packet.getData();

                    if (nBuffersReceived == 1) {
                        String addressInfo = new String(bufferRcv, 8, ByteBuffer.wrap(bufferRcv, 0, 4).getInt());
                        String dataStr[] = addressInfo.split(";");
                        logSession.logMsg("Destination: " + dataStr[0] + ":" + dataStr[1] + "\r\n");

                        logSession.logMsg("Initial time: " + initialTimeNs);
                    }

                    // extract datagram number
                    bufferInt = ByteBuffer.wrap(bufferRcv, 4, 4);
                    int datagramNumber = bufferInt.getInt();

                    // logSession.logMsg("Received buffer: " + datagramNumber);
                    // Log.d("FWD Receiver UDP", "Received datagram number: " + datagramNumber +
                    //        " from " + packet.getAddress().toString());

                    nBytesReceived += bufferRcv.length;

                    // termination condition
                    if (datagramNumber == 0) {
                        break;
                    }

                    updateVisualDeltaInformation(false, logSession);
                }

                updateVisualDeltaInformation(true, logSession);
                long finalTimeNs = lastUpdate;
                logSession.logMsg("Final time: " + finalTimeNs);
                logSession.logMsg("");

                Log.d("FWD Receiver UDP", "Ended reception from: " + packet.getAddress().toString());

                double deltaTimeSegs = (finalTimeNs - initialTimeNs) / 1_000_000_000.0;
                double receivedDataMb = (nBytesReceived * 8.0) / (1024.0 * 1024.0);
                double globalRcvSpeedMbps = receivedDataMb / deltaTimeSegs;

                // write final global average speed
                receptionGuiInfoGui.setCurAvgRcvSpeed(globalRcvSpeedMbps);

                // log data
                logSession.logMsg("Received " + nBuffersReceived + " buffers");
                logSession.logMsg("Received bytes: " + nBytesReceived);
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

                transferInfoArrayList.clear();

                receptionGuiInfoGui.setTerminatedState();
            }

        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toast(llReceptionZone, "Exception " + e.getMessage());
            llReceptionZone.post(new Runnable() {
                @Override
                public void run() {
                    clientActivity.endReceivingGuiActions();
                }
            });
        }
    }

    /*
     *
     */
    void updateVisualDeltaInformation(boolean forcedUpdate, LoggerSession logSession) {
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
    }

    @Override
    public void stopThread() {
        // stop socket listening - send a termination: a dummy local socket
        // must run in the non GUI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Socket s = new Socket(InetAddress.getByName("127.0.0.1"), portNumber);
//                    s.close();

                    DatagramSocket ds = new DatagramSocket();
                    ds.send(new DatagramPacket(new byte[0], 0,
                            InetAddress.getByName("127.0.0.1"), portNumber));
                    ds.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
