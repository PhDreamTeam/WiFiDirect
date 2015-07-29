package com.example.android.wifidirect;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.*;
import java.net.*;

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
    long rcvData = 0;

    EditText editTextSentData;
    EditText editTextRcvData;

    boolean runSender = true, runReceiver = true;
    double lastUpdate;

    Uri sourceUri;

    Thread rcvThread;

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber
            , EditText editTextSentData, EditText editTextRcvData, int bufferSize, Uri sourceUri) {
        this(destIpAddress, destPortNumber, crIpAddress, crPortNumber, 0, 0, editTextSentData, editTextRcvData,
                bufferSize, sourceUri);
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

    static NetworkInterface wifiInterface = null;

    static NetworkInterface getWLan0NetworkInterface() {
        if (wifiInterface == null) {
//            try {
//                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//
//                for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
//                    if (networkInterface.getDisplayName().equals("wlan0")) {
//                        // || networkInterface.getDisplayName().equals("eth0")) {
//                        wifiInterface = networkInterface;
//                        break;
//                    }
//                }
//            } catch (SocketException e) {
//                e.printStackTrace();
//            }

            try {
                wifiInterface = NetworkInterface.getByName("wlan0");
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return wifiInterface;
    }

    /**
     * This method gets the InetAddress for ths received address (trying to) using the wlan0 interface.
     * This is a test, to verify if we can send a message throught one speicify interface
     */
    private InetAddress getInetAddress(String destAddress) {
        try {

            // AT this code is working in ISEL
//            InetAddress dest1 = Inet6Address.getByName(destAddress);
//            Inet6Address dest = Inet6Address.getByAddress(destAddress, dest1.getAddress(), getWLan0NetworkInterface());

            // AT this code worked in iSEL (with the ipv6 address)
//            InetAddress dest = Inet6Address.getByName(destAddress);

            // to work with ipv4 and ipv6
            InetAddress dest = InetAddress.getByName(destAddress);
            return dest;

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
        if (sourceUri == null) {
            for (int i = 0; i < buffer.length; i++, b++) {
                buffer[i] = b;
            }
        }

        ContentResolver cr = null;
        InputStream is = null;
        DataOutputStream dos = null;
        Socket cliSocket = null;

        long fileSize = 0, sentData = 0;
        String fileName = null;

        try {

            if (sourceUri != null) {
                cr = editTextSentData.getContext().getContentResolver();
                is = cr.openInputStream(sourceUri);
                // get file size
                fileSize = cr.openTypedAssetFileDescriptor(sourceUri, "*/*", null).getLength();
                fileName = getFileNameFromURI(sourceUri);

                // Log.d(WiFiDirectActivity.TAG, "File URI: " + sourceUri.toString());
                Log.d(WiFiDirectActivity.TAG, "File Size: " + fileSize);
                Log.d(WiFiDirectActivity.TAG, "File Name: " + fileName);
                dataLimit = 0; // send the complete image
            }

            cliSocket = new Socket(getInetAddress(crIpAddress), crPortNumber);
            dos = new DataOutputStream(cliSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(cliSocket.getInputStream());

            // receive replies from destination
            rcvThread = createRcvThread(dis);

            // send destination information for the forward node
            String addressData = this.destIpAddress + ";" + this.destPortNumber;
            if (sourceUri != null) {
                addressData += ";" + fileName + ";" + fileSize;
            }
            //
            dos.writeInt(addressData.getBytes().length);
            dos.write(addressData.getBytes());

            Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + buffer.length);

            int dataLen = buffer.length;

            while (runSender) {
                if (is != null) {
                    dataLen = is.read(buffer);
                    if (dataLen == -1) {
                        break;
                    }
                }

                dos.write(buffer, 0, dataLen);
                sentData += dataLen;
                updateSentData(sentData, false);

                if (dataLimit != 0 && sentData > dataLimit) {
                    break;
                }
                if (speed != 0) {
                    this.sleep(speed);
                }
            }

            updateSentData(sentData, true);
            Log.d(WiFiDirectActivity.TAG, "Data sent: " + sentData);

            cliSocket.shutdownOutput();

        } catch (Exception e) {
            Log.e(WiFiDirectActivity.TAG, "Error transmitting data.");
            e.printStackTrace();
        } finally {
            // close streams
            close(is);
            close(dos);
        }
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileNameFromURI(Uri returnUri) {
        Cursor returnCursor =
                editTextSentData.getContext().getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        String fileName = returnCursor.getString(nameIndex);
        //sizeView.setText(Long.toString(returnCursor.getLong(sizeIndex)));

        returnCursor.close();

        return fileName;
    }

    private Thread createRcvThread(final DataInputStream dis) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte buffer[] = new byte[bufferSize];

                int nBytesRead = 0;

                try {
                    while (runReceiver) {
                        nBytesRead = dis.read(buffer);
                        if (nBytesRead == -1)
                            break;

                        rcvData += nBytesRead;
                    }
                } catch (IOException e) {
                    if (e.getMessage().equals("recvfrom failed: ECONNRESET (Connection reset by peer)")) {
                        // terminated with success
                        updateRcvData(true);
                        Log.d(WiFiDirectActivity.TAG,
                                "File received successfully on server, (end by exception), bytes received: " + rcvData);
                    } else {
                        Log.d(WiFiDirectActivity.TAG,"Error on File receive.");
                        e.printStackTrace();
                    }
                } finally {
                    close(dis);
                }
            }
        });
        thread.start();
        return thread;
    }

    private void updateSentData(final long sentData, boolean forceUpdate) {
        long currentNanoTime = System.nanoTime();

        if ((currentNanoTime > lastUpdate + 1000000000) || forceUpdate) {
            lastUpdate = currentNanoTime;
            editTextSentData.post(new Runnable() {
                @Override
                public void run() {
                    editTextSentData.setText("" + (sentData / 1024) + " KB");
                }
            });
            updateRcvData(forceUpdate);
        }
    }

    private void updateRcvData(boolean forceUpdate) {
        editTextRcvData.post(new Runnable() {
            @Override
            public void run() {
                editTextRcvData.setText("" + rcvData + " B");
            }
        });
    }

    @Override
    public void stopThread() {
        runSender = runReceiver = false;
        this.interrupt();
        rcvThread.interrupt();
    }
}
