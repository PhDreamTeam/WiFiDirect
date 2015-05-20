package com.example.android.wifidirect;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by dremedios on 20/05/2015.
 */
public class CrForwardServerTCP extends Thread{
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

            while(run){

                Socket cliSock = serverSocket.accept();
                new CrForwardThreadTCP(cliSock).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    void stopServer(){
        run = false;
        this.interrupt();
    }

    private class CrForwardThreadTCP extends Thread{
        Socket originSocket;
        boolean run = true;

        public CrForwardThreadTCP(Socket cliSock) {
            originSocket = cliSock;
        }

        @Override
        public void run() {
            // read initial destination data...
            // TODO read destination address and port

            // start forwarding all data to destination...
            while (run){
                // TODO forward data ...

            }

        }
        void stopForwardThread(){
            run = false;
            this.interrupt();
        }
    }
}
