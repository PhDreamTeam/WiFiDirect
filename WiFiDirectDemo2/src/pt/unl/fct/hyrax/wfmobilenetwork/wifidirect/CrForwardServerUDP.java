package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by DR AT on 28/05/2015
 * .
 */
public class CrForwardServerUDP extends Thread implements IStoppable {
    public final String TAG = "RelayServerUDP";
    private final RelayActivity relayActivity;
    private int bufferSize;
    int portNumber;
    DatagramSocket forwardRxSocket, forwardTxSocket;
    boolean run = true;
    TextView editTextTransferedData;
    long forwardedData = 0;
    long deltaForwardData = 0;
    long lastUpdate = 0;
    long initialNanoTime;

    public CrForwardServerUDP(int portNumber, TextView editTextTransferedData, int bufferSize,
                              RelayActivity relayActivity) {
        this.portNumber = portNumber;
        this.editTextTransferedData = editTextTransferedData;
        this.bufferSize = bufferSize;
        this.relayActivity = relayActivity;
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

        ByteBuffer buf = ByteBuffer.allocate(4);

        // forward Server
        try {
            forwardRxSocket = new DatagramSocket(portNumber);
            forwardTxSocket = new DatagramSocket();
            byte buffer[] = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (run) {
                forwardRxSocket.receive(packet);

                byte bufferRcv[] = packet.getData();

                int dimContentString = ByteBuffer.wrap(bufferRcv, 0, 4).getInt();
                if (dimContentString == -1) {
                    // this is a termination packet
                    break;
                }
                int bufferNumber = ByteBuffer.wrap(bufferRcv, 4, 4).getInt();
                String addressInfo = new String(bufferRcv, 8, dimContentString);
                String dataStr[] = addressInfo.split(";");

                String destinationDevice = dataStr[0];
                String destinationPort = dataStr[1];

                int nBuffersToBeReceived = ByteBuffer.wrap(bufferRcv, 8 + dimContentString, 4).getInt();

                //Log.d(TAG, "Received a datagram from " + packet.getSocketAddress() + " to " + addressInfo +
                //  ", buffer number " + bufferNumber + " of " + nBuffersToBeReceived);


                RelayRule destinationRelayRule = relayActivity.getForwardDestiny(dataStr[0]);
                if (destinationRelayRule == null) {
                    Log.d(TAG, "destIPAddress " + dataStr[0] + " with no Relay Rule, packet will be discarded");
                    return;
                }
                String destinationAddress = destinationRelayRule.ipToRelay;
                RelayRule destinationRule2 = relayActivity.getForwardDestiny(destinationAddress);
                if (destinationRule2 != null) {
                    // the destination is just a routing scheme
                    destinationDevice = destinationAddress;
                    // this is the next hop
                    destinationAddress = destinationRule2.ipToRelay;
                }

                // TODO falta fazer o bind to network

                //Log.d(TAG, "Will send previous datagram to " + destinationAddress + ":" + portNumber);

                forwardedData += bufferRcv.length;
                deltaForwardData += bufferRcv.length;

                // build packet

                String addressData = destinationDevice + ";" + destinationPort;

                // put the destination address dim
                buf.rewind();
                buf.putInt(addressData.length());
                System.arraycopy(buf.array(), 0, bufferRcv, 0, 4);

                // put buffer number
                buf.rewind();
                buf.putInt(bufferNumber);
                System.arraycopy(buf.array(), 0, bufferRcv, 4, 4);

                // send destination information for the forward node, at index 8
                System.arraycopy(addressData.getBytes(), 0, bufferRcv, 8, addressData.getBytes().length);

                // after the destination information follows the number of buffers
                buf.rewind();
                buf.putInt(nBuffersToBeReceived);
                System.arraycopy(buf.array(), 0, bufferRcv, 8 + addressData.getBytes().length, 4);

                DatagramPacket sendPacket = new DatagramPacket(bufferRcv, bufferRcv.length,
                        InetAddress.getByName(destinationAddress), portNumber);

                // send packet
                forwardTxSocket.send(sendPacket);
                updateVisualDeltaInformation(); // this may slow down reception. may want to get data only when necessary

            }
            Log.d(TAG, "UDP socket closed by user");
        } catch (SocketException e) {
            if (e.getMessage().equals("Socket closed")) {
                Log.d(TAG, "UDP socket closed by user...");
            } else e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        relayActivity.endRelayingGuiActions();
    }

    @Override
    public void stopThread() {
        run = false;
        //this.interrupt();
        forwardRxSocket.close();
    }


    void updateVisualDeltaInformation() {
        // elapsed time
        long currentNanoTime = System.nanoTime();

        if (currentNanoTime > lastUpdate + 1_000_000_000) {

            long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
            double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
            // transfer speed B/s
            double speed = (deltaForwardData / 1024) / elapsedDeltaRcvTimeSeconds;
            final String msg = String.format(Locale.US, "%d KB,  %4.2f KBps", forwardedData / 1024, speed);

            lastUpdate = currentNanoTime;
            editTextTransferedData.post(new Runnable() {
                @Override
                public void run() {
                    editTextTransferedData.setText(msg);
                }
            });
            deltaForwardData = 0;

            Log.d(TAG, msg);
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
