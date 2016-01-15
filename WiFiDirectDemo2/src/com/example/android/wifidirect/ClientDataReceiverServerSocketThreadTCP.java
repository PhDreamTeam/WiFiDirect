package com.example.android.wifidirect;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import com.example.android.wifidirect.utils.AndroidUtils;
import com.example.android.wifidirect.utils.BatteryInfo;
import com.example.android.wifidirect.utils.LoggerSession;
import com.example.android.wifidirect.utils.SystemInfo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by DR & AT on 20/05/2015.
 * .
 */
public class ClientDataReceiverServerSocketThreadTCP extends Thread implements IStoppable {
    private int bufferSize;
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;
    ReplyMode replyMode;

    LinearLayout llReceptionZone;
    Context context;
    ArrayList<IStoppable> workingThreads = new ArrayList<IStoppable>();

    public ClientDataReceiverServerSocketThreadTCP(int portNumber,
                                                   LinearLayout llReceptionZone, int bufferSize, ReplyMode replyMode) {
        this.portNumber = portNumber;
        this.bufferSize = bufferSize;
        this.llReceptionZone = llReceptionZone;
        this.replyMode = replyMode;
        context = llReceptionZone.getContext();
    }

    @Override
    public void run() {
        // receive data from other clients...
        try {
            serverSocket = new ServerSocket(portNumber);
            while (run) {
                // wait connections
                Socket cliSock = serverSocket.accept();
                AndroidUtils.toast(llReceptionZone, "Received a connection from " + cliSock.getInetAddress());
                IStoppable t = new ClientDataReceiverThreadTCP(cliSock, bufferSize, llReceptionZone, replyMode);
                workingThreads.add(t);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopThread() {
        run = false;
        // stop socket listening thread
        this.interrupt();
        // stop all client handling threads
        for (IStoppable stoppable : workingThreads)
            stoppable.stopThread();
    }

    class ClientDataReceiverThreadTCP extends Thread implements IStoppable {
        private int bufferSize;
        boolean run = true;
        Socket originSocket;
        byte buffer[];
        long nBytesReceived = 0, rcvDataCounterLastValue = 0, sentDataCounterTotal = 0;
        long initialNanoTime;
        long lastUpdate = 0;
        double maxSpeedMbps = 0;

        BatteryInfo batteryInitial;
        BatteryInfo batteryFinal;

        ReplyMode replyMode;
        ReceptionGuiInfo receptionGuiInfoGui;

        ArrayList<DataTransferInfo> transferInfoArrayList = new ArrayList<>();


        public ClientDataReceiverThreadTCP(Socket cliSock, int bufferSize, LinearLayout llReceptionZone, ReplyMode replyMode) {
            originSocket = cliSock;
            this.bufferSize = bufferSize;
            buffer = new byte[bufferSize];
            this.replyMode = replyMode;

            receptionGuiInfoGui = new ReceptionGuiInfo(llReceptionZone, cliSock.getRemoteSocketAddress().toString(),
                    cliSock.getLocalPort(), transferInfoArrayList, this);
        }

        public long getnBytesReceived() {
            return nBytesReceived;
        }

        public long getInitialNanoTime() {
            return initialNanoTime;
        }

        @Override
        public void run() {
            System.out.println(" Receiver transfer thread started...");
            OutputStream fos = null;
            DataInputStream dis = null;
            DataOutputStream dos = null;

            try {
                dis = new DataInputStream(originSocket.getInputStream());
                dos = new DataOutputStream(originSocket.getOutputStream());

                // read
                int addressLen = dis.readInt();
                dis.read(buffer, 0, addressLen);

                String addressInfo = new String(buffer, 0, addressLen);
                Log.d(WiFiDirectActivity.TAG, "Received destination address: " + addressInfo);

                // if received data is to store on file
                String aia[] = addressInfo.split(";");
                if (aia.length == 4) { // 3rd element is the file name, 4th is the file size
                    String filename = aia[2];
                    long fileSize = Long.parseLong(aia[3]);

                    SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd_HH'h'mm'm'ss's'");
                    String timestamp = sdf.format(new Date());

                    final File f = new File(Environment.getExternalStorageDirectory() + "/"
                            + context.getPackageName() + "/" + timestamp + "_" + filename); // add filename
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    //f.createNewFile();
                    Log.d(WiFiDirectActivity.TAG, "Server: saving file: " + f.toString());
                    fos = new FileOutputStream(f);

                    AndroidUtils.toast(llReceptionZone, "Receiving file on server: " + f.getAbsolutePath());

                }

                // TODO: log get battery levels
                batteryInitial = SystemInfo.getBatteryInfo(context);

                // start log session and log initial time
                LoggerSession logSession = MyMainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName() +
                        " Receiving TCP data from " + originSocket.getInetAddress().toString().substring(1));
                logSession.logTime("Initial time");

                initialNanoTime = lastUpdate = System.nanoTime();

                Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + bufferSize);

                // Receive client data
                while (run) {
                    // receive and count rcvData
                    int readDataLen = dis.read(buffer);

                    // checking end of data from socket
                    if (readDataLen == -1)
                        break;

                    // write data to file
                    if (fos != null) {
                        fos.write(buffer, 0, readDataLen);
                    }
                    nBytesReceived += readDataLen;

                    //send reply
                    if (replyMode == ReplyMode.OK) {
                        dos.write("ok".getBytes()); // send reply to original client
                        sentDataCounterTotal += "ok".getBytes().length;
                    }
                    if (replyMode == ReplyMode.ECHO) {
                        dos.write(buffer, 0, readDataLen); // send reply to original client
                        sentDataCounterTotal += readDataLen;
                    }

                    updateVisualDeltaInformation(false/*true*/); // DEBUG DR
                    // this may slow down reception. may want to get data only when necessary
                }

                long finalNanoTime = System.nanoTime();

                // log initial time
                logSession.logTime("Final time");

                // final actions
                updateVisualDeltaInformation(true);
                AndroidUtils.toast(llReceptionZone, "File received successfully on server.");
                Log.d(WiFiDirectActivity.TAG,
                        "File received successfully on server, bytes received: " + nBytesReceived);

                // calculate avg speed based on time
                double deltaTimeSegs = (finalNanoTime - initialNanoTime) / 1_000_000_000.0;
                double globalRcvSpeedMbps = (nBytesReceived * 8) / (1024.0 * 1024) / deltaTimeSegs;

                receptionGuiInfoGui.setCurAvgRcvSpeed(globalRcvSpeedMbps);

                // log data information and close log session
                logSession.logMsg("\nBytes received: " + nBytesReceived);
                logSession.logMsg("Time elapsed (s): " + String.format("%5.3f", deltaTimeSegs));
                logSession.logMsg("Global average speed (Mbps): " + String.format("%5.3f", globalRcvSpeedMbps));
                logSession.logMsg("Max rcv speed (Mbps): " + String.format("%5.3f", maxSpeedMbps));
                logSession.logMsg("Bytes sent: " + sentDataCounterTotal);
                logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
                for (DataTransferInfo data : transferInfoArrayList)
                    logSession.logMsg("  " + data);
                logSession.close();

                // remove this thread from the container of threads
                workingThreads.remove(this);

                // closing socket streams
                originSocket.shutdownOutput();
                originSocket.shutdownInput();

                // signal GUI that this connection ended
                // myToast("Received data(KB): " + receptionGui.getTvReceivedData());
                receptionGuiInfoGui.setTerminatedState();

            } catch (IOException e) {
                Log.d(WiFiDirectActivity.TAG, "Exception message: " + e.getMessage());
//                if (e.getMessage().equals("recvfrom failed: ECONNRESET (Connection reset by peer)")) {
//                    // terminated with success
//                    updateVisualDeltaInformation(true);
//                    Log.d(WiFiDirectActivity.TAG,
//                            "File received successfully on server, (end by exception), bytes received: " + nBytesReceived);
//                } else {
                // terminated with error
                Log.e(WiFiDirectActivity.TAG, "Error receiving file on server: " + e.getMessage());
                AndroidUtils.toast(llReceptionZone, "Error receiving file on server: " + e.getMessage());
                e.printStackTrace();
//                }
            } finally {
                // close socket
                try {
                    originSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                close(fos);
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, ifilter);

                batteryFinal = SystemInfo.getBatteryInfo(context);
            }
        }

        /**
         *
         */
        private void close(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         *
         */
        void updateVisualDeltaInformation(boolean forcedUpdate) {
            // elapsed time
            final long currentNanoTime = System.nanoTime();

            if ((currentNanoTime > lastUpdate + 1_000_000_000) || forcedUpdate) {

                long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
                double elapsedDeltaRcvTimeSeconds = elapsedDeltaRcvTimeNano / 1_000_000_000.0;
                long deltaReceivedBytes = nBytesReceived - rcvDataCounterLastValue;
                final double speedMbps = ((deltaReceivedBytes * 8) / (1024.0 * 1024)) / elapsedDeltaRcvTimeSeconds;
                lastUpdate = currentNanoTime;
                rcvDataCounterLastValue = nBytesReceived;

                if (speedMbps > maxSpeedMbps)
                    maxSpeedMbps = speedMbps;

                // send data to GUI
                receptionGuiInfoGui.setData(nBytesReceived / 1024.0, sentDataCounterTotal, maxSpeedMbps, speedMbps);

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


}


