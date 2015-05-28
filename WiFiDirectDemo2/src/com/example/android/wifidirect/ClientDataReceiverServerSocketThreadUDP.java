package com.example.android.wifidirect;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by DR e AT on 27/05/2015.
 *
 */
public class ClientDataReceiverServerSocketThreadUDP extends Thread implements IStopable {
    private int bufferSize;
    int portNumber;
    DatagramSocket serverDatagramSocket;
    boolean run = true;
    EditText editTextRcvData;
    byte buffer[];
    long rcvDataCounterTotal, rcvDataCounterDelta;
    long initialNanoTime;
    long lastUpdate = 0;



    public ClientDataReceiverServerSocketThreadUDP(int portNumber, EditText editTextRcvData, int bufferSize) {
        this.portNumber = portNumber;
        this.editTextRcvData = editTextRcvData;
        this.bufferSize = bufferSize;
        buffer = new byte[bufferSize];
    }


    @Override
    public void run() {
        // receive data from other clients...
        editTextRcvData.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "" + bufferSize + "KB, ClientDataReceiverServerSocketThreadUDP";
                Toast.makeText(editTextRcvData.getContext(), text, Toast.LENGTH_SHORT).show();
            }
        });

        try {
            initialNanoTime = System.nanoTime();

            serverDatagramSocket = new DatagramSocket(portNumber);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (run) {
                // wait connections
//                Log.d(WiFiDirectActivity.TAG, "Waiting for UDP datagrams");
                serverDatagramSocket.receive(packet);
//                Log.d(WiFiDirectActivity.TAG, "Received a UDP datagram");

                byte bufferRcv [] = packet.getData();
                ByteBuffer bufferInt = ByteBuffer.wrap(bufferRcv, 0, 4);
                int addressLen = bufferInt.getInt();
                String addressInfo = new String(bufferRcv,4, addressLen);
//                Log.d(WiFiDirectActivity.TAG, "Received packet with destination address: " + addressInfo);


                rcvDataCounterTotal += bufferRcv.length;
                rcvDataCounterDelta += bufferRcv.length;
                updateVisualDeltaInformation(); // this may slow down reception. may want to get data only when necessary
            }

        } catch (IOException e) {
            e.printStackTrace();
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
            final String msg = (rcvDataCounterTotal / 1024) + " KBytes " + speed + " KBps";
            lastUpdate = currentNanoTime;
            editTextRcvData.post(new Runnable() {
                @Override
                public void run() {
                    editTextRcvData.setText(msg);
                }
            });
            rcvDataCounterDelta = 0;
        }
    }

    void updateVisualInformation() {

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
