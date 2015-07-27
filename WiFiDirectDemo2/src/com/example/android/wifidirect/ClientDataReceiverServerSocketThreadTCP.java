package com.example.android.wifidirect;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by DR & AT on 20/05/2015.
 * .
 */
public class ClientDataReceiverServerSocketThreadTCP extends Thread implements IStopable{
    private int bufferSize;
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;

    EditText editTextRcvData;
    EditText editTextSendData;

    ArrayList<IStopable> workingThreads = new ArrayList<IStopable>();

    public ClientDataReceiverServerSocketThreadTCP(int portNumber, EditText editTextRcvData, EditText editTextSendData, int bufferSize) {
        this.portNumber = portNumber;
        this.editTextRcvData = editTextRcvData;
        this.editTextSendData = editTextSendData;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        editTextRcvData.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "" + bufferSize + "KB, ClientDataReceiverServerSocketThreadTCP";
                Toast.makeText(editTextRcvData.getContext(), text, Toast.LENGTH_SHORT).show();
            }
        });

        // receive data from other clients...
        try {
            serverSocket = new ServerSocket(portNumber);
            while (run) {
                // wait connections
                Socket cliSock = serverSocket.accept();
                System.out.println(" Received a connection, starting transfer thread...");
                IStopable t = new ClientDataReceiverThreadTCP(cliSock, bufferSize);
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
        this.interrupt();
        for (IStopable stopable : workingThreads)
            stopable.stopThread();
    }

    private class ClientDataReceiverThreadTCP extends Thread implements IStopable{
        private int bufferSize;
        boolean run = true;
        Socket originSocket;
        byte buffer[];
        long rcvDataCounterTotal, rcvDataCounterDelta, sendDataCounterTotal;
        long initialNanoTime;
        long lastUpdate = 0;

        public ClientDataReceiverThreadTCP(Socket cliSock, int bufferSize) {
            originSocket = cliSock;
            this.bufferSize = bufferSize;
            buffer = new byte[bufferSize];
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
            try {
                DataInputStream dis = new DataInputStream(originSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(originSocket.getOutputStream());

                int addressLen = dis.readInt();
                dis.read(buffer, 0, addressLen);

                String addressInfo = new String(buffer, 0, addressLen);
                Log.d(WiFiDirectActivity.TAG, "Received destination address: " + addressInfo);

                // if received data is to store on file
                String aia[] = addressInfo.split(";");
                if(aia.length == 3){ // 3rd element is the file name
                    String filename = aia[2];
                    Uri fileUri = Uri.parse(filename);

                    final File f = new File(Environment.getExternalStorageDirectory() + "/"
                            + editTextRcvData.getContext().getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                            + "-" + fileUri.getLastPathSegment() + ".jpg"); // add filename
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();
                    Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                    fos = new FileOutputStream(f);

                    myToast("Receiving file on server: " + f.getAbsolutePath());

                }

                // Receive client data
                initialNanoTime = System.nanoTime();
                Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + bufferSize);

                while (run) {
                    // receive and count rcvData
                    int readDataLen = dis.read(buffer);
                    if(fos != null){
                        fos.write(buffer, 0, readDataLen);
                    }
                    if (readDataLen != -1) {
                        rcvDataCounterTotal += readDataLen;
                        rcvDataCounterDelta += readDataLen;
                        //send reply
                        dos.write("ok".getBytes()); // send reply to original client
                        sendDataCounterTotal += "ok".getBytes().length;
                        updateVisualDeltaInformation(); // this may slow down reception. may want to get data only when necessary
                    } else {
                        // end of data
                        myToast("File received successfully on server.");
                        run = false;
                        dis.close();
                        originSocket.close();
                        if(fos != null)
                            fos.close();
                    }

                }

            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "Error receiving file on server: " + e.getMessage());
                myToast("Error receiving file on server: " + e.getMessage());
            }
        }

        void updateVisualDeltaInformation() {
            // elapsed time
            long currentNanoTime = System.nanoTime();

            if (currentNanoTime > lastUpdate + 1000000000) {

                long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
                double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
                // transfer speed B/s
                double speed = (rcvDataCounterDelta / 1024) / elapsedDeltaRcvTimeSeconds;
               // final String msg = (rcvDataCounterTotal / 1024) + " KBytes " + speed + " KBps";
                final String msg = String.format("%d KB %4.2f KBps", rcvDataCounterTotal / 1024,  speed);
                lastUpdate = currentNanoTime;
                editTextRcvData.post(new Runnable() {
                    @Override
                    public void run() {
                        editTextRcvData.setText(msg);
                    }
                });
                rcvDataCounterDelta = 0;
                editTextSendData.post(new Runnable() {
                    @Override
                    public void run() {
                        editTextSendData.setText("" + sendDataCounterTotal + " Bytes" );
                    }
                });
            }

        }

        void updateVisualInformation() {
            // print rcvDataCounterTotal
            // print initialNanoTime

            // elapsed time
            long currentNanoTime = System.nanoTime();

            if (currentNanoTime > lastUpdate + 1000000000) {
                long elapsedRcvTimeNano = currentNanoTime - initialNanoTime; // div 10^-9 para ter em segundos
                double elapsedRcvTimeSeconds = (double) elapsedRcvTimeNano / 1000000000.0;
                // transfer speed B/s
                double speed = (rcvDataCounterTotal / 1024) / elapsedRcvTimeSeconds;

                //update in some kind of visual component on the screen
//            System.out.println(" Total received data: " + rcvDataCounterTotal + " Bytes"
//                            + " Total elapsed time: " + elapsedRcvTimeSeconds + " seconds"
//                            + " Transfer average speed: " + speed + "Byte per second, " + speed / 8.0 + "bps"
//            );
                final String msg = (rcvDataCounterTotal / 1024) + " KBytes " + speed + " KBps";
//                    "Total received data: " + (rcvDataCounterTotal / 1024) + " KBytes"
//                            + " Total elapsed time: " + elapsedRcvTimeSeconds + " seconds"
//                            + " Transfer average speed: " + speed + " KBps, " + speed * 8.0 + " Kbps";

                lastUpdate = currentNanoTime;
                editTextRcvData.post(new Runnable() {
                    @Override
                    public void run() {
                        editTextRcvData.setText(msg);
                        ;
                    }
                });
            }

        }

        @Override
        public void stopThread() {
            run = false;
            this.interrupt();
        }
    }

    private void myToast(final String s) {
        editTextRcvData.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(editTextRcvData.getContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
