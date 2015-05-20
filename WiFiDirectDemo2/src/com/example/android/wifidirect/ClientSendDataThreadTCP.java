package com.example.android.wifidirect;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by DR & AT on 20/05/2015.
 * .
 */
public class ClientSendDataThreadTCP extends Thread{
    String destIpAddress;
    int destPortNumber;
    String crIpAddress;
    int crPortNumber;
    long speed = 0; // number of millis to sleep between each 4096 of sent Bytes
    long dataLimit = 0;
    long sentData = 0;

    boolean run = true;

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber) {
        this.destIpAddress = destIpAddress;
        this.destPortNumber = destPortNumber;
        this.crIpAddress= crIpAddress;
        this.crPortNumber = crPortNumber;
    }

    public ClientSendDataThreadTCP(String destIpAddress, int destPortNumber, String crIpAddress, int crPortNumber, long speed, long dataLimit) {
        this(destIpAddress, destPortNumber, crIpAddress, crPortNumber);
        this.speed = speed;
        this.dataLimit = dataLimit;
    }

    @Override
    public void run() {
        // Send data
        byte buffer[] = new byte[4096];
        byte b = 0;
        for (int i = 0; i < buffer.length; i++, b++) {
            buffer[i] = b;
        }
        try {
            Socket cliSocket = new Socket( crIpAddress, crPortNumber);
            DataOutputStream dos = new DataOutputStream(cliSocket.getOutputStream());

            // send destination information for the forward node
            String addressData = this.destIpAddress+";"+this.destPortNumber;
            dos.writeInt(addressData.getBytes().length);
            dos.write(addressData.getBytes());
            while(run){
                dos.write(buffer);
                sentData += buffer.length;
                if(dataLimit != 0 && sentData > dataLimit){
                    run = false;
                }
                if(speed != 0){
                    this.sleep(speed);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    void stopSendDataThread(){
        run = false;
        this.interrupt();
    }

}
