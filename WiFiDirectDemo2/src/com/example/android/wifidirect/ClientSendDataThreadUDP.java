package com.example.android.wifidirect;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.example.android.wifidirect.utils.LoggerSession;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by DR e AT on 27/05/2015
 * .
 */
public class ClientSendDataThreadUDP extends Thread implements IStoppable {
    private int bufferSizeBytes;
    String destIpAddress;
    int destPortNumber;
    String crIpAddress;
    int crPortNumber;
    long delayInterDatagramsMs = 0; // number of millis to sleep between each 4096 of sent Bytes
    long dataLimitBytes = 0;
    long nBytesSent = 0;
    TextView tvSentDataKB;
    boolean run = true;
    double lastUpdate;
    Button startStopTransmitting;

    /**
     *
     */
    public ClientSendDataThreadUDP(String destIpAddress, int destPortNumber, String crIpAddress,
                                   int crPortNumber, long delayInterDatagramsMs, long dataLimitKB,
                                   TextView tvSentDataKB, Button startStopTransmitting, int bufferSizeBytes) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress = crIpAddress;
        this.crPortNumber = crPortNumber;
        this.delayInterDatagramsMs = delayInterDatagramsMs;
        this.dataLimitBytes = dataLimitKB * 1024;
        this.tvSentDataKB = tvSentDataKB;
        this.startStopTransmitting = startStopTransmitting;
        this.bufferSizeBytes = bufferSizeBytes;
    }


    @Override
    public void run() {
        // get number of buffers to send
        int nBuffersToSend = (int) (dataLimitBytes / bufferSizeBytes) +
                (dataLimitBytes % bufferSizeBytes != 0 ? 1 : 0);

        // start log session and log initial time
        LoggerSession logSession = MyMainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName());
        logSession.logMsg("Send data to CR: " + crIpAddress + ":" + crPortNumber);
        logSession.logMsg("Send data to dest: " + destIpAddress + ":" + destPortNumber + "\n");
        long initialTxTimeMs = logSession.logTime("Initial time");

        // Send data buffer and fill with some values
        byte buffer[] = new byte[bufferSizeBytes];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) i;
        }

        try {
            InetAddress iadCrIpAddress = InetAddress.getByName(crIpAddress);

            DatagramSocket cliSocket = new DatagramSocket();
            String addressData = this.destIpAddress + ";" + this.destPortNumber;

            // first 4 bytes contains the string length
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.putInt(addressData.length());
            System.arraycopy(buf.array(), 0, buffer, 0, 4);

            // send destination information for the forward node, at index 8
            System.arraycopy(addressData.getBytes(), 0, buffer, 8, addressData.getBytes().length);

            Log.d(WiFiDirectActivity.TAG, "Using BufferSize: " + bufferSizeBytes);

            DatagramPacket packet;

            while (run) {
                // second 4 bytes contains the number fo the datagram, reverse order, last will be zero
                buf = ByteBuffer.allocate(4);
                buf.putInt(--nBuffersToSend);
                System.arraycopy(buf.array(), 0, buffer, 4, 4);

                // build packet and send it
                packet = new DatagramPacket(buffer, buffer.length, iadCrIpAddress, crPortNumber);
                cliSocket.send(packet);
                Log.d("WFD Sender UDP", "Sent datagram number: " + nBuffersToSend);

                nBytesSent += buffer.length;

                updateSentData(nBytesSent, false);

                if (dataLimitBytes != 0 && nBytesSent >= dataLimitBytes) {
                    run = false;
                }

                if (delayInterDatagramsMs != 0) {
                    Thread.sleep(delayInterDatagramsMs);
                }
            }

            // update gui with last results and state button
            updateSentData(nBytesSent, true);

            // log end writing time
            long finalTxTimeMs = logSession.logTime("Final sent time");

            // log final sent and receive bytes
            logSession.logMsg("Data sent (B): " + nBytesSent + ", (MB): " + nBytesSent / (1024.0 * 1024));
            double deltaTimeSegs = (finalTxTimeMs - initialTxTimeMs) / 1000.0;
            double sentDataMb = ((double) (nBytesSent * 8)) / (1024 * 1204);
            double globalSentSpeedMbps =  sentDataMb / deltaTimeSegs;
            logSession.logMsg("Data sent speed (Mbps): " + String.format("%5.3f", globalSentSpeedMbps));
            logSession.close();

            tvSentDataKB.post(new Runnable() {
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
            tvSentDataKB.post(new Runnable() {
                @Override
                public void run() {
                    tvSentDataKB.setText("" + (sentData / 1024));
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
