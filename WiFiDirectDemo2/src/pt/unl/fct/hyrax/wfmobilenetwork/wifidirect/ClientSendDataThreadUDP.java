package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.net.Network;
import android.util.Log;
import android.widget.TextView;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by DR e AT on 27/05/2015
 * .
 */
public class ClientSendDataThreadUDP extends Thread implements IStoppable {
    public static final String LOG_TAG = ClientActivity.TAG + " UDP";
    private final Network network;

    private int bufferSizeBytes;
    UDPSocket socketUDP;
    String destIpAddress;
    int destPortNumber;
    String crIpAddress;
    int crPortNumber;
    long delayInterDatagramsMs = 0; // number of millis to sleep between each 4096 of sent Bytes
    long nBytesToSend = 0;
    long nBytesSent = 0;
    TextView tvSentDataKB;
    boolean run = true;
    long lastUpdate;
    ClientActivity clientActivity;
    private float nextNotificationValue = 0.1f;
    private long txDataCounterLastValue;
    private double maxDataSendSpeedMbps;

    /**
     *
     */
    public ClientSendDataThreadUDP(String destIpAddress, int destPortNumber, UDPSocket sock, long delayInterDatagramsMs,
                                   long dataLimitKB, TextView tvSentDataKB, ClientActivity clientActivity,
                                   int bufferSizeBytes, Network network) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.socketUDP = sock;
        this.crIpAddress = sock.getCrIpAddress();
        this.crPortNumber = sock.getCrPort();
        this.delayInterDatagramsMs = delayInterDatagramsMs;
        this.nBytesToSend = dataLimitKB * 1024;
        this.tvSentDataKB = tvSentDataKB;
        this.clientActivity = clientActivity;
        this.bufferSizeBytes = bufferSizeBytes;
        this.network = network;
    }


    @Override
    public void run() {

        AndroidUtils.toast(tvSentDataKB, "Start transmitting!!!!!");
        Log.d(LOG_TAG, "Start transmission to CR " + crIpAddress + ":" + crPortNumber + " with dest " +
                destIpAddress + ":" + destPortNumber);

        // get number of buffers to send
        int nBuffersToSend = (int) (nBytesToSend / bufferSizeBytes) +
                (nBytesToSend % bufferSizeBytes != 0 ? 1 : 0);

        // start log session and log initial time
        LoggerSession logSession = MainActivity.logger.getNewLoggerSession(this.getClass().getSimpleName(),
                clientActivity.getLogDir());
        logSession.logMsg("Send data to CR: " + crIpAddress + ":" + crPortNumber);
        logSession.logMsg("Send data to dest: " + destIpAddress + ":" + destPortNumber + "\r\n");

        // Send data buffer and fill with some values
        byte buffer[] = new byte[bufferSizeBytes];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) i;
        }

        DatagramSocket cliSocket = socketUDP.getSocket();
        ByteBuffer buf = ByteBuffer.allocate(4);

        try {
            InetAddress iadCrIpAddress = InetAddress.getByName(crIpAddress);

            if(network != null) {
                network.bindSocket(cliSocket);
            }

            String addressData = this.destIpAddress + ";" + this.destPortNumber;

            // first 4 bytes contains the string length
            buf.rewind();
            buf.putInt(addressData.length());
            System.arraycopy(buf.array(), 0, buffer, 0, 4);

            // send destination information for the forward node, at index 8
            System.arraycopy(addressData.getBytes(), 0, buffer, 8, addressData.getBytes().length);

            // after the destination information follows the number of buffers
            buf.rewind();
            buf.putInt((int) (nBytesToSend / bufferSizeBytes));
            System.arraycopy(buf.array(), 0, buffer, 8 + addressData.getBytes().length, 4);

            Log.d(LOG_TAG, "Sending data (B): " + nBytesToSend + ", with BufferSize (B): " + buffer.length);

            DatagramPacket packet;

            logSession.logTime("Initial time");
            long initialTxTimeNs = System.nanoTime();

            while (run) {
                // second 4 bytes contains the number fo the datagram, reverse order, last will be zero
                buf.rewind();
                buf.putInt(--nBuffersToSend);
                System.arraycopy(buf.array(), 0, buffer, 4, 4);

                // build packet and send it
                packet = new DatagramPacket(buffer, buffer.length, iadCrIpAddress, crPortNumber);
                cliSocket.send(packet);
                // Log.d(ClientActivity.TAG, "Transmitter UDP: sent datagram number: " + nBuffersToSend);

                nBytesSent += buffer.length;

                updateSentData(nBytesSent, false);

                if (nBytesToSend != 0 && nBytesSent >= nBytesToSend) {
                    break;
                }

                if (delayInterDatagramsMs != 0) {
                    Thread.sleep(delayInterDatagramsMs);
                }
            }

            // send forced termination packets
            Log.d(LOG_TAG, "Sending termination packets");
            buf.rewind();
            buf.putInt(-1);
            System.arraycopy(buf.array(), 0, buffer, 0, 4);
            packet = new DatagramPacket(buffer, buffer.length, iadCrIpAddress, crPortNumber);
            cliSocket.send(packet);
            sleep(100);
            cliSocket.send(packet);
            sleep(100);
            cliSocket.send(packet);
            sleep(100);
            cliSocket.send(packet);
            sleep(100);
            cliSocket.send(packet);

            if (!run) {
                logSession.logMsg("Transmission stopped, by user action");
                Log.d(LOG_TAG, "Transmission stopped, by user action");
                cliSocket.close();
                logSession.close(tvSentDataKB.getContext());
                tvSentDataKB.post(new Runnable() {
                    public void run() {
                        clientActivity.endTransmittingGuiActions(null);
                    }
                });
                return;
            }

            // log end writing time
            logSession.logTime("Final sent time");
            long finalTxTimeNs = System.nanoTime();

            // update gui with last results and state button
            updateSentData(nBytesSent, true);

            // log final sent and receive bytes
            logSession.logMsg("Data sent (B): " + nBytesSent + ", (MB): " + nBytesSent / (1024.0 * 1024));
            double deltaTimeSegs = (finalTxTimeNs - initialTxTimeNs) / 1_000_000_000.0;
            double sentDataMb = (nBytesSent * 8.0) / (1024.0 * 1024.0);
            double globalSentSpeedMbps = sentDataMb / deltaTimeSegs;
            logSession.logMsg("Data sent speed (Mbps): " + String.format("%5.3f", globalSentSpeedMbps));
            logSession.logMsg("Data sent Max speed (Mbps): " + String.format("%5.3f", maxDataSendSpeedMbps));
            Log.d(LOG_TAG, "End of transmission, data sent: " + nBytesSent);

            cliSocket.close();
            tvSentDataKB.post(new Runnable() {
                public void run() {
                    clientActivity.endTransmittingGuiActions(null);
                }
            });

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error transmitting data: " + e.getMessage());
            e.printStackTrace();
            AndroidUtils.toast(tvSentDataKB, "Error transmitting data: " + e.getMessage());
            logSession.logMsg("Transmission stopped by user - GUI");
        } finally {
            AndroidUtils.close(cliSocket);
        }

        logSession.close(tvSentDataKB.getContext());
    }

    /*
     *
     */
    private void updateSentData(final long sentData, boolean forceUpdate) {
        long currentNanoTime = System.nanoTime();

        if (forceUpdate || currentNanoTime > lastUpdate + 1000000000) {
            long elapsedDeltaTxTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
            double elapsedDeltaTxTimeSeconds = elapsedDeltaTxTimeNano / 1_000_000_000.0;
            long deltaTxBytes = sentData - txDataCounterLastValue;
            final double speedMbps = ((deltaTxBytes * 8) / (1024.0 * 1024)) / elapsedDeltaTxTimeSeconds;

            // exclude last reading
            if (!forceUpdate && speedMbps > maxDataSendSpeedMbps)
                maxDataSendSpeedMbps = speedMbps;

            lastUpdate = currentNanoTime;
            tvSentDataKB.post(new Runnable() {
                @Override
                public void run() {
                    tvSentDataKB.setText("" + (sentData / 1024));
                }
            });

            txDataCounterLastValue = sentData;
        }

        // ADB console notification
        float bytesSentPercentage = sentData / (float) nBytesToSend;
        if (bytesSentPercentage > nextNotificationValue) {
            Log.d(LOG_TAG, "Bytes sent: " + String.format("%.1f", bytesSentPercentage * 100) + "%");
            nextNotificationValue += 0.1f;
        }

    }


    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
    }

}
