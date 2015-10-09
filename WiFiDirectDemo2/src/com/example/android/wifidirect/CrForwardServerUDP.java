package com.example.android.wifidirect;

import android.widget.TextView;
import android.widget.Toast;

import java.net.*;
import java.nio.ByteBuffer;

/**
 * Created by DR AT on 28/05/2015.
 *
 *
 */
public class CrForwardServerUDP  extends Thread implements IStoppable {
    private int bufferSize;
    int portNumber;
    DatagramSocket forwardRxSocket, forwardTxSocket ;
    boolean run = true;
    TextView editTextTransferedData;
    long forwardedData = 0;
    long deltaForwardData = 0;
    long lastUpdate = 0;
    long initialNanoTime;

    public CrForwardServerUDP(int portNumber, TextView editTextTransferedData, int bufferSize) {
        this.portNumber = portNumber;
        this.editTextTransferedData = editTextTransferedData;
        this.bufferSize = bufferSize;
    }


    @Override
    public void run() {

        editTextTransferedData.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "" + bufferSize + "KB, CrForwardServerUDP";
                Toast.makeText(editTextTransferedData.getContext(), text, Toast.LENGTH_SHORT).show();
            }
        });



        // forward Server
        try {
            forwardRxSocket = new DatagramSocket(portNumber);
            forwardTxSocket = new DatagramSocket();
            byte buffer[] = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (run) {
                forwardRxSocket.receive(packet);
//                Log.d(WiFiDirectActivity.TAG, "Received a UDP datagram");
                byte bufferRcv [] = packet.getData();
                ByteBuffer bufferInt = ByteBuffer.wrap(bufferRcv, 0, 4);
                int addressLen = bufferInt.getInt();
                String addressInfo = new String(bufferRcv,4, addressLen);
                String aa[] = addressInfo.split(";");
//                Log.d(WiFiDirectActivity.TAG, "Received packet with destination address: " + addressInfo);

                forwardedData += bufferRcv.length;
                deltaForwardData += bufferRcv.length;
                DatagramPacket sendPacket = new DatagramPacket(bufferRcv, bufferRcv.length, InetAddress.getByName(aa[0]), Integer.parseInt(aa[1]));
                forwardTxSocket.send(sendPacket);
                updateVisualDeltaInformation(); // this may slow down reception. may want to get data only when necessary
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
