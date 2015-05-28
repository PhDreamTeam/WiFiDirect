package com.example.android.wifidirect;

import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by DR & AT on 20/05/2015.
 *
 */
public class CrForwardServerTCP extends Thread implements IStopable{
    private int bufferSize;
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;
    TextView editTextTransferedData;

    public CrForwardServerTCP(int portNumber, TextView editTextTransferedData, int bufferSize) {
        this.portNumber = portNumber;
        this.editTextTransferedData = editTextTransferedData;
        this.bufferSize = bufferSize;
    }


    @Override
    public void run() {
        // forward Server
        try {
            serverSocket = new ServerSocket(portNumber);

            while (run) {

                Socket cliSock = serverSocket.accept();
                new CrForwardThreadTCP(cliSock, bufferSize).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
    }

    private class CrForwardThreadTCP extends Thread implements IStopable{
        private int bufferSize;
        Socket originSocket;
        boolean run = true;
        long forwardedData = 0;
        long deltaForwardData = 0;
        long lastUpdate = 0;
        long initialNanoTime;

        public CrForwardThreadTCP(Socket cliSock, int bufferSize) {
            this.originSocket = cliSock;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            editTextTransferedData.post(new Runnable() {
                @Override
                public void run() {
                    CharSequence text = "" + bufferSize + "KB, CrForwardThreadTCP";
                    Toast.makeText(editTextTransferedData.getContext(), text, Toast.LENGTH_SHORT).show();
                }
            });

            byte buffer[] = new byte[bufferSize];
            try {
                // read initial destination data...
                DataInputStream dis = new DataInputStream(originSocket.getInputStream());

                int addressInfoLen = dis.readInt();
                dis.read(buffer, 0, addressInfoLen);
                String addressInfo = new String(buffer, 0, addressInfoLen);
                String aia[] = addressInfo.split(";");

                String destIpAddress = aia[0];
                int destPortNumber = Integer.parseInt(aia[1]);

                // open destination socket
                Socket destSocket = new Socket(destIpAddress, destPortNumber);
                DataOutputStream dos = new DataOutputStream(destSocket.getOutputStream());

                // firstly send packet destination address in case of needed by another cr
                dos.writeInt(addressInfoLen);
                dos.write(buffer, 0, addressInfoLen);

                Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + buffer.length);

                initialNanoTime = System.nanoTime();
                // start forwarding all data to destination...
                while (run) {
                    int nBytesRead = dis.read(buffer);
                    if (nBytesRead != -1) {
                        dos.write(buffer, 0, nBytesRead);
                        forwardedData += nBytesRead;
                        deltaForwardData += nBytesRead;
                        updateVisualDeltaInformation();
                    } else
                        run = false;
                }
                dos.close();
                dis.close();
                originSocket.close();
                destSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void stopThread() {
            run = false;
            this.interrupt();
        }

        void updateVisualDeltaInformation() {
            // elapsed time
            long currentNanoTime = System.nanoTime();

            if (currentNanoTime > lastUpdate + 1000000000) {

                long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
                double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
                // transfer speed B/s
                double speed = (deltaForwardData / 1024) / elapsedDeltaRcvTimeSeconds;
                final String msg = (forwardedData / 1024) + " KBytes " + speed + " KBps";
                lastUpdate = currentNanoTime;
                editTextTransferedData.post(new Runnable() {
                    @Override
                    public void run() {
                        editTextTransferedData.setText(msg);
                    }
                });
                deltaForwardData = 0;
            }

        }
        void updateVisualInformation() {
            // elapsed time
            long currentNanoTime = System.nanoTime();

            if (currentNanoTime > lastUpdate + 1000000000) {
                long elapsedRcvTimeNano = currentNanoTime - initialNanoTime; // div 10^-9 para ter em segundos
                double elapsedRcvTimeSeconds = (double) elapsedRcvTimeNano / 1000000000.0;
                // transfer speed B/s
                double speed = (forwardedData / 1024) / elapsedRcvTimeSeconds;
                final String msg = (forwardedData / 1024) + " KBytes " + speed + " KBps";
                lastUpdate = currentNanoTime;
                editTextTransferedData.post(new Runnable() {
                    @Override
                    public void run() {
                        editTextTransferedData.setText(msg);
                    }
                });
            }

        }
    }
}
