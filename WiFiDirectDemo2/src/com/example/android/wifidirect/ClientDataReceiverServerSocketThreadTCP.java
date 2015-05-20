package com.example.android.wifidirect;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by dremedios on 20/05/2015.
 */
public class ClientDataReceiverServerSocketThreadTCP extends Thread{
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;

    public ClientDataReceiverServerSocketThreadTCP(int portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public void run() {
        // receive data from other clients...
        try {
            serverSocket = new ServerSocket(portNumber);
            while(run){
                // wait connections
                Socket cliSock = serverSocket.accept();
                System.out.println(" Received a connection, starting transfer thread...");
                new ClientDataReceiverThreadTCP(cliSock).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopClientDataReceiver(){
        run = false;
        this.interrupt();
    }

    private class ClientDataReceiverThreadTCP extends Thread{
        boolean run = true;
        Socket originSocket;
        byte buffer[] = new byte[4096];
        long rcvDataCounterTotal;
        long initialNanoTime;

        public ClientDataReceiverThreadTCP(Socket cliSock) {
            originSocket = cliSock;
        }

        public long getRcvDataCounterTotal() {
            return rcvDataCounterTotal;
        }

        public long getInitialNanoTime() {
            return initialNanoTime;
        }

        @Override
        public void run() {
            System.out.println(" Receiver transfer thread started...");
            try {
                InputStream is = originSocket.getInputStream();
                // Receive client data
                initialNanoTime = System.nanoTime();
                while (run){
                    // receive and count rcvData
                    int readData = is.read(buffer);
                    if( readData != -1) {
                        rcvDataCounterTotal += readData;
                        updateVisualInformation(); // this may slow down reception. may want to get data only when necessary
                    } else {
                        // end of data
                        run = false;
                        is.close();
                        originSocket.close();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void updateVisualInformation(){
            // print rcvDataCounterTotal
            // print initialNanoTime

            // elapsed time
            long currentNanoTime = System.nanoTime();
            long elapsedRcvTimeNano = currentNanoTime - initialNanoTime; // div 10^-9 para ter em segundos
            double elapsedRcvTimeSeconds = (double) elapsedRcvTimeNano / 1000000000.0;

            // transfer speed B/s
            double speed = rcvDataCounterTotal / elapsedRcvTimeSeconds;

            //update in some kind of visual component on the screen
            System.out.println(" Total received data: " + rcvDataCounterTotal + " Bytes"
                            + " Total elapsed time: " + elapsedRcvTimeSeconds + " seconds"
                            + " Transfer average speed: " + speed + "Byte per second, " + speed / 8.0 + "bps"
            );

        }

        void stopClientDataReceiverThread(){
            run = false;
            this.interrupt();
        }
    }
}
