package com.example.android.wifidirect;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by DR e AT on 27/05/2015.
 * .
 */
public class ClientSendDataThreadUDP extends Thread implements IStoppable {
    private int bufferSize;
    String destIpAddress;
    int destPortNumber;
    String crIpAddress;
    int crPortNumber;
    long speed = 0; // number of millis to sleep between each 4096 of sent Bytes
    long dataLimit = 0;
    long sentData = 0;
    EditText editTextSentData;
    boolean run = true;
    double lastUpdate;
    Button startStopTransmitting;

    /**
     *
     */
    public ClientSendDataThreadUDP(String destIpAddress, int destPortNumber, String crIpAddress,
                                   int crPortNumber, long speed, long dataLimit,
                                   EditText editTextSentData,
                                   Button startStopTransmitting, int bufferSize) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.speed = speed;
        this.dataLimit = dataLimit * 1024;
        this.editTextSentData = editTextSentData;
        this.startStopTransmitting = startStopTransmitting;
        this.bufferSize = bufferSize;
    }


    @Override
    public void run() {
        // Send data
        byte buffer[] = new byte[bufferSize];
        byte b = 0;
        for (int i = 0; i < buffer.length; i++, b++) {
            buffer[i] = b;
        }
        try {

            DatagramSocket cliSocket = new DatagramSocket();
            // send destination information for the forward node
            String addressData = this.destIpAddress + ";" + this.destPortNumber;

            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.putInt(addressData.length());
            System.arraycopy(buf.array(), 0, buffer, 0, 4);
            System.arraycopy(addressData.getBytes(), 0, buffer, 4, addressData.getBytes().length);

            Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + bufferSize);

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(crIpAddress), crPortNumber);

            while (run) {
                cliSocket.send(packet);
                sentData += buffer.length;

                updateSentData(sentData, false);

                if (dataLimit != 0 && sentData >= dataLimit) {
                    run = false;
                }

                if (speed != 0) {
                    Thread.sleep(speed);
                }
            }

            // update gui with last results and state button
            updateSentData(sentData, true);

            editTextSentData.post(new Runnable() {
                public void run() {
                    startStopTransmitting.setText("Start Transmitting");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     *
     */
    private void updateSentData(final long sentData, boolean forceUpdate) {
        long currentNanoTime = System.nanoTime();

        if (forceUpdate || currentNanoTime > lastUpdate + 1000000000) {
            lastUpdate = currentNanoTime;
            editTextSentData.post(new Runnable() {
                @Override
                public void run() {
                    editTextSentData.setText("" + (sentData / 1024) + " KB");
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
