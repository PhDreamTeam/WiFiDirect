package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.util.Log;
import android.widget.LinearLayout;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.AndroidUtils;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by DR e AT on 27/05/2015.
 * .
 */
public class ClientDataReceiverServerSocketThreadUDP extends Thread implements IStoppable {
    static final String LOG_TAG = "FWD Receiver UDP";

    private ArrayList<UDPSession> udpSessions = new ArrayList<>();
    private ClientActivity clientActivity;
    private DatagramSocket serverDatagramSocket;
    private LinearLayout llReceptionZone;
    private int localPortNumber;
    private int bufferSizeBytes;
    private byte buffer[];


    /**
     *
     */
    public ClientDataReceiverServerSocketThreadUDP(UDPSocket serverSocket, LinearLayout llReceptionZone,
                                                   int bufferSizeBytes, ClientActivity clientActivity) {
        this.llReceptionZone = llReceptionZone;
        this.clientActivity = clientActivity;
        this.bufferSizeBytes = bufferSizeBytes;

        serverDatagramSocket = serverSocket.getSocket();
        localPortNumber = serverSocket.getCrPort();

        buffer = new byte[bufferSizeBytes];
    }

    /**
     * Do datagram package distribution by the several occurring transmissions
     */
    @Override
    public void run() {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            Log.d(LOG_TAG, "Starting UDP reception server at port: " + localPortNumber);

            while (true) {
                // wait for datagrams
                serverDatagramSocket.receive(packet);

                String senderIpAddress = packet.getAddress().toString().substring(1);

                // check for end of reception
                if (senderIpAddress.equals("127.0.0.1")) {

                    // end of receiving actions, called by user
                    AndroidUtils.toast(llReceptionZone, "Reception stopped");
                    // close datagram socket
                    serverDatagramSocket.close();

                    // signal current transmissions to finish
                    for (UDPSession session : udpSessions) {
                        if (!session.isFinishedTransmission())
                            session.finishTransmissionByLocalUser();
                    }

                    Log.d(LOG_TAG, "Stopping reception from GUI action");

                    // stop receiving cycle
                    break;
                }

                // get the UDP sessions (or a new one) for this sender (ip & port) and process packet on it
                UDPSession session = getUdpSession(senderIpAddress, packet.getPort());
                session.processUDPPackage(packet);
            }

        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toast(llReceptionZone, "Exception " + e.getMessage());
        }

        // do end of receiving gui actions on client activity
        llReceptionZone.post(new Runnable() {
            @Override
            public void run() {
                clientActivity.endReceivingGuiActions();
            }
        });
    }

    /**
     *
     */
    private UDPSession getUdpSession(String senderIpAddress, int senderPort) {
        // look for an existing session, if one have the same sender properties return it
        for (UDPSession s : udpSessions) {
            if (s.haveSameSender(senderIpAddress, senderPort))
                return s;
        }

        // no existing session found, create a new one, add with to current sessions and return it
        UDPSession session = new UDPSession(senderIpAddress, senderPort, localPortNumber, clientActivity,
                llReceptionZone, bufferSizeBytes);
        udpSessions.add(session);

        return session;
    }


    /**
     *
     */
    @Override
    public void stopThread() {
        // stop socket listening - send a termination: a dummy local datagram
        // must run in the non GUI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket ds = new DatagramSocket();
                    ds.send(new DatagramPacket(new byte[0], 0,
                            InetAddress.getByName("127.0.0.1"), localPortNumber));
                    ds.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

