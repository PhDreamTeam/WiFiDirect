package com.example.android.wifidirect;

import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by DR e AT on 27/05/2015.
 */
public class ClientSendDataThreadUDP extends Thread implements IStopable {
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

    public ClientSendDataThreadUDP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, EditText editTextSentData, int bufferSize) {
        this(destIpAddress, destPortNumber, crIpAddress, crPortNumber, 0, 0, editTextSentData, bufferSize);
    }

    public ClientSendDataThreadUDP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, long speed, long dataLimit, EditText editTextSentData, int bufferSize) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.speed = speed;
        this.dataLimit = dataLimit * 1024;
        this.editTextSentData = editTextSentData;
        this.bufferSize = bufferSize;
    }


    @Override
    public void run() {
        editTextSentData.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "" + bufferSize + "KB, ClientSendDataThreadUDP";
                Toast.makeText(editTextSentData.getContext(), text, Toast.LENGTH_SHORT).show();
            }
        });

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

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(crIpAddress), crPortNumber);

            Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + bufferSize);

            while (run) {
                cliSocket.send(packet);
                sentData += buffer.length;
                updateSentData(sentData);
                if (dataLimit != 0 && sentData > dataLimit) {
                    run = false;
                }
                if (speed != 0) {
                    this.sleep(speed);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSentData(final long sentData) {
        long currentNanoTime = System.nanoTime();

        if (currentNanoTime > lastUpdate + 1000000000) {
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
