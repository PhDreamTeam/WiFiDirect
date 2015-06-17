package com.example.android.wifidirect;

import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by DR & AT on 20/05/2015.
 * .
 */
public class ClientSendDataThreadTCP extends Thread implements IStopable {
    private int bufferSize;
    String destIpAddress;
    int destPortNumber;
    String crIpAddress;
    int crPortNumber;
    long speed = 0; // number of millis to sleep between each 4096 of sent Bytes
    long dataLimit = 0;
    long sentData = 0, rcvData = 0;

    EditText editTextSentData;
    EditText editTextRcvData;

    boolean run = true;
    double lastUpdate;

    Thread rcvThread;

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, EditText editTextSentData, EditText editTextRcvData, int bufferSize) {
        this(destIpAddress, destPortNumber, crIpAddress, crPortNumber, 0, 0, editTextSentData, editTextRcvData, bufferSize);
    }

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, long speed, long dataLimit, EditText editTextSentData, EditText editTextRcvData, int bufferSize) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.speed = speed;
        this.dataLimit = dataLimit * 1024;
        this.editTextSentData = editTextSentData;
        this.editTextRcvData = editTextRcvData;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        editTextSentData.post(new Runnable() {
            @Override
            public void run() {
                CharSequence text = "" + bufferSize + "KB, ClientSendDataThreadTCP";
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

            Socket cliSocket = new Socket(crIpAddress, crPortNumber);
            DataOutputStream dos = new DataOutputStream(cliSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(cliSocket.getInputStream());

            // receive replys from destination
            rcvThread = createRcvThread(dis);

            // send destination information for the forward node
            String addressData = this.destIpAddress + ";" + this.destPortNumber;
            dos.writeInt(addressData.getBytes().length);
            dos.write(addressData.getBytes());

            Log.d( WiFiDirectActivity.TAG, "Using BufferSize: " + buffer.length);

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

    private Thread createRcvThread(final DataInputStream dis) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte buffer[] = new byte[bufferSize];

                int nBytesRead = 0;

                try {
                    while (run) {
                        nBytesRead = dis.read(buffer);

                        if (nBytesRead != -1) {
                            rcvData += nBytesRead;
                        } else
                            run = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return thread;
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
            editTextRcvData.post(new Runnable() {
                @Override
                public void run() {
                    editTextRcvData.setText("" + rcvData + " B");
                }
            });
        }
    }

    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
        rcvThread.interrupt();
    }
}
