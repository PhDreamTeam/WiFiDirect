package com.example.android.wifidirect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by DR & AT on 20/05/2015.
 *
 */
public class CrForwardServerTCP extends Thread {
    int portNumber;
    ServerSocket serverSocket;
    boolean run = true;

    public CrForwardServerTCP(int portNumber) {
        this.portNumber = portNumber;
    }

    public CrForwardServerTCP() {
        this.portNumber = 20000;
    }

    @Override
    public void run() {
        // forward Server
        try {
            serverSocket = new ServerSocket(portNumber);

            while (run) {

                Socket cliSock = serverSocket.accept();
                new CrForwardThreadTCP(cliSock).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    void stopServer() {
        run = false;
        this.interrupt();
    }

    private class CrForwardThreadTCP extends Thread {
        Socket originSocket;
        boolean run = true;

        public CrForwardThreadTCP(Socket cliSock) {
            originSocket = cliSock;
        }

        @Override
        public void run() {
            byte buffer[] = new byte[4096];
            try {
                // read initial destination data...
                DataInputStream dis = new DataInputStream(originSocket.getInputStream());

                int addressInfoLen = dis.readInt();
                dis.read(buffer, 0, addressInfoLen);
                String addressInfo = new String(buffer, 0, addressInfoLen);
                String aia[] = addressInfo.split(";");

                String destIpAddress = aia[0];
                int destPortNumber = Integer.parseInt(aia[1]);

                // open destination socket
                Socket destSocket = new Socket( destIpAddress, destPortNumber);
                DataOutputStream dos = new DataOutputStream(destSocket.getOutputStream());

                // firstly send packet destination address in case of needed by another cr
                dos.write(buffer, 0, addressInfoLen);

                // start forwarding all data to destination...
                while (run) {
                    int nBytesRead = dis.read(buffer);
                    if(nBytesRead != -1)
                        dos.write(buffer, 0, nBytesRead);
                    else
                        run = false;
                }
                dos.close();
                dis.close();
                originSocket.close();
                destSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void stopForwardThread() {
            run = false;
            this.interrupt();
        }
    }
}
