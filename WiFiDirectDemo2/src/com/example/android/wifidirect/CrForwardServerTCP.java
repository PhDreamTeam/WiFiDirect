package com.example.android.wifidirect;

import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by DR & AT on 20/05/2015.
 */
public class CrForwardServerTCP extends Thread implements IStopable{
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;
    TextView editTextTransferedData;

    public CrForwardServerTCP(int portNumber, TextView editTextTransferedData) {
        this.portNumber = portNumber;
        this.editTextTransferedData = editTextTransferedData;
    }

    public CrForwardServerTCP() {
        this.portNumber = 20000;
    }

    @Override
    public void run() {
        // forward Server
        try {
            serverSocket = new ServerSocket(portNumber);

            while (run) {

                Socket cliSock = serverSocket.accept();
                new CrForwardThreadTCP(cliSock).start();
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
        Socket originSocket;
        boolean run = true;
        long forwardedData = 0;
        long lastUpdate = 0;
        long initialNanoTime;

        public CrForwardThreadTCP(Socket cliSock) {
            this.originSocket = cliSock;
        }

        @Override
        public void run() {
            byte buffer[] = new byte[CommonDefinitions.BUFFER_SIZE];
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

                Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + CommonDefinitions.BUFFER_SIZE);

                initialNanoTime = System.nanoTime();
                // start forwarding all data to destination...
                while (run) {
                    int nBytesRead = dis.read(buffer);
                    if (nBytesRead != -1) {
                        dos.write(buffer, 0, nBytesRead);
                        forwardedData += nBytesRead;
                        updateVisualInformation();
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
