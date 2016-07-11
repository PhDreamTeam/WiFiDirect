package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.net.Network;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import pt.unl.fct.hyrax.wfmobilenetwork.wifidirect.utils.LoggerSession;

import javax.net.SocketFactory;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by DR & AT on 20/05/2015
 * .
 */
public class CrForwardServerTCP extends Thread implements IStoppable {
    public final String TAG = "RelayServerTCP";
    private final RelayActivity relayActivity;
    private int bufferSize;
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;
    TextView textViewTransferedDataOrigDest, textViewTransferedDataDestOrig;
    ArrayList<IStoppable> workingThreads = new ArrayList<>();

    /**
     *
     */
    public CrForwardServerTCP(int portNumber, TextView textViewTransferedDataOrigDest
            , TextView textViewTransferedDataDestOrig, int bufferSize,
                              RelayActivity relayActivity) {
        this.portNumber = portNumber;
        this.textViewTransferedDataOrigDest = textViewTransferedDataOrigDest;
        this.textViewTransferedDataDestOrig = textViewTransferedDataDestOrig;
        this.bufferSize = bufferSize;

        this.relayActivity = relayActivity;
    }

    /**
     *
     */
    private Socket getSocket(ClientActivity.BIND_TO_NETWORK bindToNetwork, String crIpAddress, int crPortNumber) {
        try {
            if (ClientActivity.BIND_TO_NETWORK.WF.equals(bindToNetwork)) {

                // bind to WF network
                Network networkWifi = relayActivity.getNetworkWF();
                Log.d(TAG, "Bind socket to network: WF " + networkWifi);
                SocketFactory sf = networkWifi.getSocketFactory();
                return sf.createSocket(ClientSendDataThreadTCP.getInetAddress(crIpAddress), crPortNumber);

            } else if (ClientActivity.BIND_TO_NETWORK.WFD.equals(bindToNetwork)) {
                throw new RuntimeException("get socket called to get WFD interface: not supported...");
            } else {

                // InetAddress localIpAddress = InetAddress.getByName("192.168.49.241");
                return new Socket(ClientSendDataThreadTCP.getInetAddress(crIpAddress), crPortNumber);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void run() {
        // forward Server
        Socket cliSock = null;
        try {
            serverSocket = new ServerSocket(portNumber);

            while (run) {
                cliSock = serverSocket.accept();
                IStoppable thd = new CrForwardThreadTCP(cliSock, bufferSize);
                workingThreads.add(thd);
                thd.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            stopThread();
            relayActivity.endRelayingGuiActions();
            if (cliSock != null)
                try {
                    cliSock.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
    }

    /**
     *
     */
    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (IStoppable stopable : workingThreads)
            stopable.stopThread();
    }

    /**
     *
     */
    private class CrForwardThreadTCP extends Thread implements IStoppable {
        private int bufferSize;
        Socket originSocket;
        Socket destSocket;
        boolean run = true;
        Thread threadForwardDataOrigDest = null;
        Thread threadForwardDataDestOrig = null;
        DataInputStream origDIS;
        DataOutputStream origDOS;
        DataOutputStream destDOS;
        DataInputStream destDIS;
        private LoggerSession logSession;

        public CrForwardThreadTCP(Socket cliSock, int bufferSize) {
            this.originSocket = cliSock;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
//            textViewTransferedDataOrigDest.post(new Runnable() {
//                @Override
//                public void run() {
//                    CharSequence text = "" + bufferSize + "KB, CrForwardThreadTCP";
//                    Toast.makeText(textViewTransferedDataOrigDest.getContext(), text, Toast.LENGTH_SHORT).show();
//                }
//            });

            byte buffer[] = new byte[bufferSize];
            String destIpAddress = null;
            int destPortNumber = 0;

            try {
                // read initial destination data...
                origDIS = new DataInputStream(originSocket.getInputStream());
                origDOS = new DataOutputStream(originSocket.getOutputStream());

                int addressInfoLen = origDIS.readInt();
                origDIS.read(buffer, 0, addressInfoLen);
                String addressInfo = new String(buffer, 0, addressInfoLen);
                String aia[] = addressInfo.split(";");
                long bytesToSent = origDIS.readLong();

                destIpAddress = aia[0];
                destPortNumber = Integer.parseInt(aia[1]);

                String relayAddress = relayActivity.getForwardDestiny(destIpAddress);
                if (relayAddress != null) {
                    destIpAddress = relayAddress;
                    destPortNumber = 30000; //default CR PORT TODO CHANGE THIS TO A DYNAMIC PORT
                }

                String msg = " Received a connection from " + originSocket.getInetAddress().toString().substring(1) +
                        ":" + originSocket.getPort() + " to " + addressInfo + ", will be sent to " +
                        destIpAddress + ":" + destPortNumber + ", with bytes: " + bytesToSent;

                Log.v(TAG, msg);

                // start log session and log initial time
                logSession = MainActivity.logger.getNewLoggerSession(
                        this.getClass().getSimpleName() + msg, relayActivity.getLogDir());

                //logSession.logMsg("Destination: " + aia[0] + ":" + aia[1] + "\r\n");
                logSession.logTime("Initial time");
                logSession.startLoggingBatteryValues(relayActivity);


                // open destination socket
                destSocket = getSocket(relayActivity.getCurrentBindToNetwork(), destIpAddress, destPortNumber); // new Socket(destIpAddress, destPortNumber);
                destDOS = new DataOutputStream(destSocket.getOutputStream());
                destDIS = new DataInputStream(destSocket.getInputStream());

                // firstly send packet destination address in case of needed by another cr
                destDOS.writeInt(addressInfoLen);
                destDOS.write(buffer, 0, addressInfoLen);
                destDOS.writeLong(bytesToSent);

                threadForwardDataOrigDest = forwardData("FWD", origDIS, destDOS, destSocket, textViewTransferedDataOrigDest);
                threadForwardDataDestOrig = forwardData("BCK", destDIS, origDOS, originSocket, textViewTransferedDataDestOrig);

                Log.d(TAG, "Using BufferSize: " + buffer.length);

                threadForwardDataOrigDest.join();
                threadForwardDataDestOrig.join();


                // log end common values
                logSession.stopLoggingBatteryValues();
                logSession.logMsg("");
                logSession.logBatteryConsumedJoules();
                logSession.close(relayActivity);

            } catch (Exception e) {
                Log.d(TAG, "Error opening relay channel to: " + destIpAddress + ":" + destPortNumber);

                e.printStackTrace();
            } finally {
                closeCloseable(destDOS);
                closeCloseable(origDIS);
                closeCloseable(originSocket);
                closeCloseable(destSocket);
            }
        }

        /**
         *
         */
        public void closeCloseable(Closeable closeable) {
            try {
                if (closeable != null)
                    closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         *
         */
        private Thread forwardData(final String direction, final DataInputStream dis, final DataOutputStream dos,
                                   final Socket destSocket, final TextView textView) {
            Thread thread = new Thread(new Runnable() {
                double maxSpeedMbps;

                @Override
                public void run() {
                    byte buffer[] = new byte[bufferSize];
                    long lastUpdate = 0;
                    long initialNanoTime;
                    long forwardedData = 0;
                    long deltaForwardData = 0;
                    int nBytesRead = 0;
                    int nBuffersReceived = 0;

                    initialNanoTime = System.nanoTime();

                    long initialTxTimeMs = logSession.logTime(direction + " Initial time");

                    // start forwarding all data to destination...
                    try {
                        while (run) {
                            nBytesRead = dis.read(buffer);
                            Log.v(TAG, direction + " Received a buffer nº " + ++nBuffersReceived +
                                    ", with bytes: " + nBytesRead);

                            if (nBytesRead == -1)
                                break;

                            dos.write(buffer, 0, nBytesRead);
                            forwardedData += nBytesRead;
                            deltaForwardData += nBytesRead;
                            long updatedLastTime = updateVisualDeltaInformation(
                                    forwardedData, deltaForwardData, lastUpdate, textView);
                            if (updatedLastTime != 0) {
                                deltaForwardData = 0; // if updated clear delta counter
                                lastUpdate = updatedLastTime; // new updated inf timestamp
                            }
                        }

                        // send EOT to destination
                        destSocket.shutdownOutput();

                        // to avoid mix data from forward and backward threads
                        synchronized (CrForwardServerTCP.this) {
                            // end log session
                            // log end writing time
                            long finalTxTimeMs = logSession.logTime("\n" + direction + " Final sent time");

                            double deltaTimeSegs = (finalTxTimeMs - initialTxTimeMs) / 1000.0;
                            logSession.logMsg(direction + " Time elapsed (s): " + String.format("%5.3f", deltaTimeSegs));

                            // log final sent and receive bytes
                            logSession.logMsg(direction + " Data forwarded (B): " + forwardedData + ", (MB): " + forwardedData / (1024.0 * 1024));
                            double sentDataMb = ((double) (forwardedData * 8)) / (1024 * 1024);
                            double dataSentSpeedMbps = sentDataMb / deltaTimeSegs;
                            logSession.logMsg(direction + " Data forwarded speed (Mbps): " + String.format("%5.3f", dataSentSpeedMbps));
                            logSession.logMsg(direction + " Data forwarded Max speed (Mbps): " + String.format("%5.3f", maxSpeedMbps));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // returns 0 if not updated, else return the lastUpdateTime
                long updateVisualDeltaInformation(long forwardedData, long deltaForwardData, long lastUpdate, final TextView textView) {
                    // elapsed time
                    long currentNanoTime = System.nanoTime();

                    if (currentNanoTime > lastUpdate + 1000000000) {
                        long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
                        double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
                        final double speedMbps = ((deltaForwardData * 8) / (1024.0 * 1024)) / elapsedDeltaRcvTimeSeconds;

                        if (speedMbps > maxSpeedMbps) {
                            maxSpeedMbps = speedMbps;
                        }

                        //final String msg = (forwardedData / 1024) + " KBytes " + speed + " Mbps";
                        final String msg = String.format("%d KB,  %4.2f Mbps", forwardedData / 1024, speedMbps);
                        lastUpdate = currentNanoTime;
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(msg);
                            }
                        });

                        Log.d(TAG, msg);
                        return lastUpdate;
                    }
                    return 0;
                }
            });
            thread.start();
            return thread;
        }

        @Override
        public void stopThread() {
            run = false;
            threadForwardDataOrigDest.interrupt();
            threadForwardDataDestOrig.interrupt();

            try {
                origDIS.close();
                destDIS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
