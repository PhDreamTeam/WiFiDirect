package com.example.android.wifidirect;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.LinearLayout;
import com.example.android.wifidirect.utils.AndroidUtils;
import com.example.android.wifidirect.utils.BatteryInfo;
import com.example.android.wifidirect.utils.LoggerSession;
import com.example.android.wifidirect.utils.SystemInfo;

import java.io.*;
import java.net.InetAddress;
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
    public static final String LOG_TAG = ClientActivity.TAG + " TCP";
    static private String FILES_DIR_PATH = MyMainActivity.APP_MAIN_FILES_DIR_PATH + "/files";

    private int bufferSize;
    int portNumber;
    ServerSocket serverSocket;
    ReplyMode replyMode;

    LinearLayout llReceptionZone;
    ClientActivity clientActivity;
    Context context;
    ArrayList<IStoppable> workingThreads = new ArrayList<IStoppable>();
    File filesDirPath;

    public ClientDataReceiverServerSocketThreadTCP(int portNumber,
                                                   LinearLayout llReceptionZone, int bufferSize,
                                                   ReplyMode replyMode, ClientActivity clientActivity) {
        this.portNumber = portNumber;
        this.bufferSize = bufferSize;
        this.llReceptionZone = llReceptionZone;
        this.replyMode = replyMode;
        context = llReceptionZone.getContext();
        this.clientActivity = clientActivity;

        filesDirPath = new File(FILES_DIR_PATH);
        AndroidUtils.buildPath(filesDirPath.toString());
    }

    @Override
    public void run() {
        // receive data from other clients...
        try {
            serverSocket = new ServerSocket(portNumber);
            Log.d(LOG_TAG, "Starting TCP reception server at port: " + portNumber);
            while (true) {
                // wait connections
                Socket cliSock = serverSocket.accept();

                // checking for stopping condition
                if (cliSock.getInetAddress().toString().equals("/127.0.0.1")) {
                    AndroidUtils.toast(llReceptionZone, "Stopping TCP reception");
                    Log.d(LOG_TAG, "Stopping reception server at port: " + portNumber);
                    // close server socket
                    serverSocket.close();
                    return;
                } else {
                    String transmitterIP = cliSock.getInetAddress().toString().substring(1);
                    String msg = "Received a connection from " + transmitterIP + ":" + cliSock.getPort();
                    AndroidUtils.toast(llReceptionZone, msg);
                    Log.d(LOG_TAG, msg);
                }

                IStoppable t = new ClientDataReceiverThreadTCP(cliSock, bufferSize, llReceptionZone, replyMode);
                workingThreads.add(t);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toast(llReceptionZone, "Exception " + e.getMessage());
            Log.d(LOG_TAG, "Exception " + e.getMessage());
            llReceptionZone.post(new Runnable() {
                @Override
                public void run() {
                    clientActivity.endReceivingGuiActions();
                }
            });
        }
    }

    /*
     * Stop reception
     */
    public void stopThread() {
        Log.d(LOG_TAG, "Stopping reception from GUI action");

        // stop socket listening - send a termination: a dummy local socket
        // must run in the non GUI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket s = new Socket(InetAddress.getByName("127.0.0.1"), portNumber);
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // stop all client handling threads
        for (IStoppable stoppable : workingThreads)
            stoppable.stopThread();
    }

    /**
     *
     */
    class ClientDataReceiverThreadTCP extends Thread implements IStoppable {
        private int bufferSize;
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

        private String originIP;
        private int originPort;

        ArrayList<DataTransferInfo> transferInfoArrayList = new ArrayList<>();


        /*
         *
         */
        public ClientDataReceiverThreadTCP(Socket cliSock, int bufferSize, LinearLayout llReceptionZone, ReplyMode replyMode) {
            originSocket = cliSock;
            this.bufferSize = bufferSize;
            buffer = new byte[bufferSize];
            this.replyMode = replyMode;

            originIP = originSocket.getInetAddress().toString().substring(1);
            originPort = originSocket.getPort();

            receptionGuiInfoGui = new ReceptionGuiInfo("TCP", llReceptionZone,
                    cliSock.getRemoteSocketAddress().toString(),
                    cliSock.getLocalPort(), transferInfoArrayList, this);

            clientActivity.registerReceptionGuiInfo(receptionGuiInfoGui);
        }

        /*
         *
         */
        public long getnBytesReceived() {
            return nBytesReceived;
        }

        /*
         *
         */
        public long getInitialNanoTime() {
            return initialNanoTime;
        }

        /*
         *
         */
        @Override
        public void run() {
            OutputStream fos = null;
            DataInputStream dis = null;
            DataOutputStream dos = null;

            LoggerSession logSession = null;

            try {
                dis = new DataInputStream(originSocket.getInputStream());
                dos = new DataOutputStream(originSocket.getOutputStream());

                // read
                int addressLen = dis.readInt();
                dis.read(buffer, 0, addressLen);

                String addressInfo = new String(buffer, 0, addressLen);
                int firstIdx = addressInfo.indexOf(';');
                String destIPAddress = addressInfo.substring(0, firstIdx);
                int secondIdx = addressInfo.indexOf(addressInfo.indexOf(';', firstIdx + 1), ';');
                secondIdx = secondIdx == -1 ? addressInfo.length() : secondIdx;
                String destPort = addressInfo.substring(firstIdx + 1, secondIdx);
                Log.d(LOG_TAG,
                        "Received a connection from " + originIP + ":" + originPort +
                                " with destination: " + destIPAddress + ":" + destPort);

                // if received data is to store on file
                String aia[] = addressInfo.split(";");
                if (aia.length == 4) { // 3rd element is the file name, 4th is the file size
                    String filename = aia[2];
                    //long fileSize = Long.parseLong(aia[3]);
                    String timestamp = new SimpleDateFormat("yy-MM-dd_HH'h'mm'm'ss's'").format(new Date());

                    final File f = new File(filesDirPath + "/" + timestamp + "_" + filename); // add filename
                    Log.d(LOG_TAG, "Server: saving file: " + f.toString());
                    fos = new FileOutputStream(f);
                    AndroidUtils.toast(llReceptionZone, "Receiving file on server: " + f.getAbsolutePath());
                }

                // TODO: log get battery levels
                batteryInitial = SystemInfo.getBatteryInfo(context);

                // start log session and log initial time
                logSession = MyMainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName() +
                                " Receiving TCP data from " + originSocket.getInetAddress().toString().substring(1),
                        clientActivity.getLogDir());

                logSession.logMsg("Destination: " + aia[0] + ":" + aia[1] + "\r\n");
                logSession.logTime("Initial time");

                initialNanoTime = lastUpdate = System.nanoTime();

                Log.d(LOG_TAG, "Using BufferSize: " + bufferSize);

                // Receive client data
                while (true) {
                    // receive and count rcvData
                    int readDataLen = dis.read(buffer);

                    // checking end of data from socket
                    if (readDataLen == -1) {
                        break;
                    }

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
                String msg = "EOT OK, Reception from " + originIP + ":" + originPort +
                        ", received (B): " + nBytesReceived;
                AndroidUtils.toast(llReceptionZone, msg);
                Log.d(LOG_TAG, msg);

                // calculate avg speed based on time
                double deltaTimeSegs = (finalNanoTime - initialNanoTime) / 1_000_000_000.0;
                double globalRcvSpeedMbps = (nBytesReceived * 8) / (1024.0 * 1024) / deltaTimeSegs;

                receptionGuiInfoGui.setCurAvgRcvSpeed(globalRcvSpeedMbps);

                // log data information and close log session
                logSession.logMsg("\nBytes received: " + nBytesReceived);
                logSession.logMsg("Time elapsed (s): " + String.format("%5.3f", deltaTimeSegs));
                logSession.logMsg("Global average speed (Mbps): " + String.format("%5.3f", globalRcvSpeedMbps));
                logSession.logMsg("Max rcv speed (Mbps): " + String.format("%5.3f", maxSpeedMbps));
                logSession.logMsg("Bytes sent: " + sentDataCounterTotal + "\r\n");
                logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
                for (DataTransferInfo data : transferInfoArrayList)
                    logSession.logMsg("  " + data);
                logSession.close(llReceptionZone.getContext());

                // remove this thread from the container of threads
                workingThreads.remove(this);

                // closing socket streams
                originSocket.shutdownOutput();
                originSocket.shutdownInput();

                // signal GUI that this connection ended
                // myToast("Received data(KB): " + receptionGui.getTvReceivedData());
                receptionGuiInfoGui.setTerminatedState();

            } catch (IOException e) {
                // reception terminated not normally
                String msg =  "Reception from: " + originIP + ":" + originPort + " stopped, cause: " +
                        (e.getMessage().equals("Socket closed") ? "by user action" : e.getMessage());
                Log.e(LOG_TAG, msg);
                AndroidUtils.toast(llReceptionZone, msg);
                receptionGuiInfoGui.setTerminatedState(" - err");
                // e.printStackTrace();

                if (logSession != null) {
                    logSession.logMsg(msg);
                    logSession.logMsg("Receiving history - speedMbps, deltaTimeSegs, deltaMBytes: ");
                    for (DataTransferInfo data : transferInfoArrayList)
                        logSession.logMsg("  " + data);
                    logSession.close(llReceptionZone.getContext());
                }
            } finally {
                // close socket
                if (originSocket != null && !originSocket.isInputShutdown())
                    close(originSocket);
                close(fos);
                IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, iFilter);

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

                // exclude last reading
                if (!forcedUpdate && speedMbps > maxSpeedMbps)
                    maxSpeedMbps = speedMbps;

                // send data to GUI
                receptionGuiInfoGui.setData(nBytesReceived / 1024.0, sentDataCounterTotal, maxSpeedMbps, speedMbps);

                transferInfoArrayList.add(new DataTransferInfo(speedMbps, elapsedDeltaRcvTimeSeconds,
                        deltaReceivedBytes, currentNanoTime));
            }
        }

        @Override
        public void stopThread() {
            // run = false;
            //this.interrupt();
            // a better way to force termination is to use:
            try {
                originSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}


