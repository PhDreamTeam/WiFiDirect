package com.example.android.wifidirect;

import android.widget.EditText;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by DR & AT on 20/05/2015.
 * .
 */
public class ClientSendDataThreadTCP extends Thread {
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

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, EditText editTextSentData) {
        this(destIpAddress, destPortNumber, crIpAddress, crPortNumber, 0, 0, editTextSentData);
    }

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, long speed, long dataLimit, EditText editTextSentData) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.speed = speed;
        this.dataLimit = dataLimit * 1024;
        this.editTextSentData = editTextSentData;

    }

    @Override
    public void run() {
        // Send data
        byte buffer[] = new byte[4096];
        byte b = 0;
        for (int i = 0; i < buffer.length; i++, b++) {
            buffer[i] = b;
        }
        try {
            Socket cliSocket = new Socket(crIpAddress, crPortNumber);
            DataOutputStream dos = new DataOutputStream(cliSocket.getOutputStream());

            // send destination information for the forward node
            String addressData = this.destIpAddress + ";" + this.destPortNumber;
            dos.writeInt(addressData.getBytes().length);
            dos.write(addressData.getBytes());

            while (run) {
                dos.write(buffer);
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

    void stopSendDataThread() {
        run = false;
        this.interrupt();
    }

}
