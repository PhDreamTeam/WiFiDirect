package com.example.android.wifidirect;

import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

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

    LinearLayout llReceptionZone;
    ArrayList<IStoppable> workingThreads = new ArrayList<IStoppable>();

    public ClientDataReceiverServerSocketThreadTCP(int portNumber,
                                                   LinearLayout llReceptionZone, int bufferSize) {
        this.portNumber = portNumber;
        this.bufferSize = bufferSize;
        this.llReceptionZone = llReceptionZone;
    }

    @Override
    public void run() {
        // receive data from other clients...
        try {
            serverSocket = new ServerSocket(portNumber);
            while (run) {
                // wait connections
                Socket cliSock = serverSocket.accept();
                myToast("Received a connection...");
                IStoppable t = new ClientDataReceiverThreadTCP(cliSock, bufferSize, llReceptionZone);
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

    private class ClientDataReceiverThreadTCP extends Thread implements IStoppable {
        private int bufferSize;
        boolean run = true;
        Socket originSocket;
        byte buffer[];
        long rcvDataCounterTotal, rcvDataCounterLastValue, sentDataCounterTotal;
        long initialNanoTime;
        long lastUpdate = 0;
        double maxSpeed = 0;

        ReceptionGuiInfo receptionGuiInfoGui;

        public ClientDataReceiverThreadTCP(Socket cliSock, int bufferSize, LinearLayout llReceptionZone) {
            originSocket = cliSock;
            this.bufferSize = bufferSize;
            buffer = new byte[bufferSize];

            receptionGuiInfoGui = new ReceptionGuiInfo(llReceptionZone, cliSock.getRemoteSocketAddress().toString(),
                    cliSock.getLocalPort());
        }

        public long getRcvDataCounterTotal() {
            return rcvDataCounterTotal;
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
                            + llReceptionZone.getContext().getPackageName() + "/" + timestamp + "_" + filename); // add filename
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    //f.createNewFile();
                    Log.d(WiFiDirectActivity.TAG, "Server: saving file: " + f.toString());
                    fos = new FileOutputStream(f);

                    myToast("Receiving file on server: " + f.getAbsolutePath());

                }

                initialNanoTime = System.nanoTime();
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

                    rcvDataCounterTotal += readDataLen;

                    //send reply
                    dos.write("ok".getBytes()); // send reply to original client
                    sentDataCounterTotal += "ok".getBytes().length;

                    updateVisualDeltaInformation(false/*true*/); // DEBUG DR
                    // this may slow down reception. may want to get data only when necessary
                }

                long finalNanoTime = System.nanoTime();

                // final actions
                updateVisualDeltaInformation(true);
                myToast("File received successfully on server.");
                Log.d(WiFiDirectActivity.TAG,
                        "File received successfully on server, bytes received: " + rcvDataCounterTotal);

                // calculate avg speed  based on time
                long deltaNanoTime = finalNanoTime - initialNanoTime;
                receptionGuiInfoGui.setCurAvgRcvSpeed((rcvDataCounterTotal / 1024.0) /
                        (deltaNanoTime / 1000000000.0));

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
//                            "File received successfully on server, (end by exception), bytes received: " + rcvDataCounterTotal);
//                } else {
                // terminated with error
                Log.e(WiFiDirectActivity.TAG, "Error receiving file on server: " + e.getMessage());
                myToast("Error receiving file on server: " + e.getMessage());
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

            if ((currentNanoTime > lastUpdate + 1000000000) || forcedUpdate) {

                long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
                double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
                // transfer speed B/s
                final double speed = ((sentDataCounterTotal - rcvDataCounterLastValue) / 1024)
                        / elapsedDeltaRcvTimeSeconds;
                lastUpdate = currentNanoTime;
                rcvDataCounterLastValue = sentDataCounterTotal;

                if (speed > maxSpeed)
                    maxSpeed = speed;

                receptionGuiInfoGui.setData(rcvDataCounterTotal / 1024,
                        sentDataCounterTotal, maxSpeed, speed);
            }
        }


        @Override
        public void stopThread() {
            run = false;
            this.interrupt();
        }
    }

    private void myToast(final String s) {
        llReceptionZone.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(llReceptionZone.getContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }
}


