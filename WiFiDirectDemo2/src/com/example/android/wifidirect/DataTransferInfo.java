package com.example.android.wifidirect;

/**
 * Created by DR & AT on 24/11/2015
 * .
 */
class DataTransferInfo {
    double deltaTimeSegs;
    double deltaMBytes;
    long currentNanoTime;

    public DataTransferInfo(long deltaTimeNanoSegs, double deltaBytes, long currentNanoTime) {
        this.deltaTimeSegs = deltaTimeNanoSegs  / 1000_000_000.0;
        this.deltaMBytes = deltaBytes / (1024.0 * 1024);
        this.currentNanoTime = currentNanoTime;
    }

    @Override
    public String toString() {
        double speedMbps = (deltaMBytes * 8) / deltaTimeSegs;
        return String.format("%5.2f, %3.1f, %5.2f", speedMbps, deltaTimeSegs, deltaMBytes);
    }

    public String toStringDetailed() {
        double speedMbps = (deltaMBytes * 8) / deltaTimeSegs;
        return String.format("%5.9f, %3.9f, %5.9f", speedMbps, deltaTimeSegs, deltaMBytes);
    }
}
