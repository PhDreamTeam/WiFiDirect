package com.example.android.wifidirect;

import android.widget.LinearLayout;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

/**
 * Created by DR e AT on 27/05/2015.
 * .
 */
public class ClientDataReceiverServerSocketThreadUDP extends Thread implements IStoppable {
    private int bufferSize;
    int portNumber;
    DatagramSocket serverDatagramSocket;
    boolean run = true;
    LinearLayout llReceptionZone;
    byte buffer[];
    long rcvDataCounterTotal, rcvDataCounterLastValue;
    long initialNanoTime;
    long lastUpdate = 0;
    private ReceptionGuiInfo receptionGuiInfoGui;
    double maxSpeed = 0;

    ArrayList<DataTransferInfo> transferInfoArrayList = new ArrayList<>();

    public ClientDataReceiverServerSocketThreadUDP(int portNumber,
                                                   LinearLayout llReceptionZone, int bufferSize) {
        this.llReceptionZone = llReceptionZone;
        this.portNumber = portNumber;
        this.bufferSize = bufferSize;
        buffer = new byte[bufferSize];
    }


    @Override
    public void run() {
        try {
            initialNanoTime = System.nanoTime();

            serverDatagramSocket = new DatagramSocket(portNumber);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            receptionGuiInfoGui = new ReceptionGuiInfo(llReceptionZone, "...", portNumber, transferInfoArrayList, null);

            while (run) {
                // wait connections
                serverDatagramSocket.receive(packet);

                byte bufferRcv[] = packet.getData();
//                ByteBuffer bufferInt = ByteBuffer.wrap(bufferRcv, 0, 4);
//                int addressLen = bufferInt.getInt();
//                String addressInfo = new String(bufferRcv, 4, addressLen);

                rcvDataCounterTotal += bufferRcv.length;
                updateVisualDeltaInformation();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void updateVisualDeltaInformation() {
        // elapsed time
        long currentNanoTime = System.nanoTime();

        if (currentNanoTime > lastUpdate + 1000000000) {

            long elapsedDeltaRcvTimeNano = currentNanoTime - lastUpdate; // div 10^-9 para ter em segundos
            double elapsedDeltaRcvTimeSeconds = (double) elapsedDeltaRcvTimeNano / 1000000000.0;
            // transfer speed B/s
            double speed = ((rcvDataCounterTotal - rcvDataCounterLastValue) / 1024) /
                    elapsedDeltaRcvTimeSeconds;

            if (speed > maxSpeed)
                maxSpeed = speed;

            receptionGuiInfoGui.setData(rcvDataCounterTotal / 1024, 0, maxSpeed, speed);
            rcvDataCounterLastValue = rcvDataCounterTotal;
            lastUpdate = currentNanoTime;
        }
    }

    @Override
    public void stopThread() {
        run = false;
        this.interrupt();
    }
}
