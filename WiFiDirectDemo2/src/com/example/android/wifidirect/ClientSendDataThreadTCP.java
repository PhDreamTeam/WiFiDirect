package com.example.android.wifidirect;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;

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

    Uri sourceUri;

    Thread rcvThread;

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber
            , EditText editTextSentData, EditText editTextRcvData, int bufferSize, Uri sourceUri) {
        this(destIpAddress, destPortNumber, crIpAddress, crPortNumber, 0, 0, editTextSentData, editTextRcvData, bufferSize, sourceUri);
    }

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber
            , long speed, long dataLimit, EditText editTextSentData, EditText editTextRcvData, int bufferSize
            , Uri sourceUri) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.speed = speed;
        this.dataLimit = dataLimit * 1024;
        this.editTextSentData = editTextSentData;
        this.editTextRcvData = editTextRcvData;
        this.bufferSize = bufferSize;
        this.sourceUri = sourceUri;
    }

    private InetAddress getInetAddress(String destAddress) {
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface wifiInterface = null;
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.getDisplayName().equals("wlan0")) { // || networkInterface.getDisplayName().equals("eth0")) {
                    wifiInterface = networkInterface;
                    break;
                }
            }
            Log.d("dest address", destAddress + " -> " + Arrays.toString(destAddress.getBytes()));
//            Inet6Address dest = Inet6Address.getByAddress(null, destAddress.getBytes(), wifiInterface );
            InetAddress dest = Inet6Address.getByName(destAddress);
            return dest;

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
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

        // if no inputstream, fill dummy data
        if(sourceUri == null) {
            for (int i = 0; i < buffer.length; i++, b++) {
                buffer[i] = b;
            }
        }

        ContentResolver cr = null;
        InputStream is = null;
        DataOutputStream dos = null;

        try {

            if(sourceUri != null) {
                cr = editTextSentData.getContext().getContentResolver();
                is = cr.openInputStream(sourceUri);
                dataLimit = 0; // send the complete image
            }

            Socket cliSocket = new Socket(getInetAddress(crIpAddress), crPortNumber);
            dos = new DataOutputStream(cliSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(cliSocket.getInputStream());

            // receive replies from destination
            rcvThread = createRcvThread(dis);

            // send destination information for the forward node
            String addressData = this.destIpAddress + ";" + this.destPortNumber;
            if(sourceUri != null){
                addressData += ";" + sourceUri.toString();
            }
             //
            dos.writeInt(addressData.getBytes().length);
            dos.write(addressData.getBytes());

            Log.d( WiFiDirectActivity.TAG, "Using BufferSize: " + buffer.length);

            int dataLen = buffer.length;
            while (run) {

                if(is != null){
                    dataLen = is.read(buffer);
                    if(dataLen == -1) {
                        run = false;
                        break;
                    }
                }

                dos.write(buffer, 0, dataLen);
                sentData += dataLen;
                updateSentData(sentData);
                if (dataLimit != 0 && sentData > dataLimit) {
                    run = false;
                }
                if (speed != 0) {
                    this.sleep(speed);
                }
            }
        } catch (Exception e) {
            Log.e(WiFiDirectActivity.TAG, "Error transmitting data.");
            e.printStackTrace();
        } finally {
            if(is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, "Error closing input stream from source transmitting file.");
                }
            // DEBUG DR
//            if(dos != null)
//                try {
//                    dos.close();
//                } catch (IOException e) {
//                    Log.e(WiFiDirectActivity.TAG, "Error transmitting data.");
//                }
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
