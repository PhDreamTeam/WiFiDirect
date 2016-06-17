package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

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

    public CrForwardServerTCP(int portNumber, TextView textViewTransferedDataOrigDest
            , TextView textViewTransferedDataDestOrig, int bufferSize,
                              RelayActivity relayActivity) {
        this.portNumber = portNumber;
        this.textViewTransferedDataOrigDest = textViewTransferedDataOrigDest;
        this.textViewTransferedDataDestOrig = textViewTransferedDataDestOrig;
        this.bufferSize = bufferSize;

        this.relayActivity = relayActivity;
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
            if(cliSock != null)
                try {
                    cliSock.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
    }

    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
        for (IStoppable stopable : workingThreads)
            stopable.stopThread();
    }

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

        public CrForwardThreadTCP(Socket cliSock, int bufferSize) {
            this.originSocket = cliSock;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            textViewTransferedDataOrigDest.post(new Runnable() {
                @Override
                public void run() {
                    CharSequence text = "" + bufferSize + "KB, CrForwardThreadTCP";
                    Toast.makeText(textViewTransferedDataOrigDest.getContext(), text, Toast.LENGTH_SHORT).show();
                }
            });

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

                destIpAddress = aia[0];
                destPortNumber = Integer.parseInt(aia[1]);

                String relayAddress = relayActivity.getForwardDestiny(destIpAddress);
                if (relayAddress != null) {
                    destIpAddress = relayAddress;
                    destPortNumber = 30000; //default CR PORT TODO CHANGE THIS TO A DYNAMIC PORT
                }
                // open destination socket
                destSocket = new Socket(destIpAddress, destPortNumber);
                destDOS = new DataOutputStream(destSocket.getOutputStream());
                destDIS = new DataInputStream(destSocket.getInputStream());

                // firstly send packet destination address in case of needed by another cr
                destDOS.writeInt(addressInfoLen);
                destDOS.write(buffer, 0, addressInfoLen);

                threadForwardDataOrigDest = forwardData(origDIS, destDOS, textViewTransferedDataOrigDest);
                threadForwardDataDestOrig = forwardData(destDIS, origDOS, textViewTransferedDataDestOrig);

                Log.d(TAG, "Using BufferSize: " + buffer.length);

                threadForwardDataOrigDest.join();
                threadForwardDataDestOrig.join();

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

        public void closeCloseable(Closeable closeable) {
            try {
                if(closeable != null)
                    closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Thread forwardData(final DataInputStream dis, final DataOutputStream dos, final TextView textView) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte buffer[] = new byte[bufferSize];
                    long lastUpdate = 0;
                    long initialNanoTime;
                    long forwardedData = 0;
                    long deltaForwardData = 0;
                    int nBytesRead = 0;

                    initialNanoTime = System.nanoTime();
                    // start forwarding all data to destination...
                    try {
                        while (run) {
                            nBytesRead = dis.read(buffer);

                            if (nBytesRead != -1) {
                                dos.write(buffer, 0, nBytesRead);
                                forwardedData += nBytesRead;
                                deltaForwardData += nBytesRead;
                                long updatedLastTime = updateVisualDeltaInformation(
                                        forwardedData, deltaForwardData, lastUpdate, textView);
                                if (updatedLastTime != 0) {
                                    deltaForwardData = 0; // if updated clear delta counter
                                    lastUpdate = updatedLastTime; // new updated inf timestamp
                                }
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

        @Override
        public void stopThread() {
            run = false;
            threadForwardDataOrigDest.interrupt();
            threadForwardDataDestOrig.interrupt();
        }

        // returns 0 if not updated, else return the lastUpdateTime
        long updateVisualDeltaInformation(long forwardedData, long deltaForwardData, long lastUpdate, final TextView textView) {
            // elapsed time
            long currentNanoTime = System.nanoTime();

            if (currentNanoTime > lastUpdate + 1000000000) {
                long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
                double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
                // transfer speed B/s
                double speed = (deltaForwardData / 1024) / elapsedDeltaRcvTimeSeconds;
                //final String msg = (forwardedData / 1024) + " KBytes " + speed + " KBps";
                final String msg = String.format("%d KB,  %4.2f KBps", forwardedData / 1024, speed);
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
    }
}
