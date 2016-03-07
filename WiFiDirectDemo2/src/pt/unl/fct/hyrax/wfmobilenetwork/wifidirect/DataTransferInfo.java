package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

/**
 * Created by DR & AT on 24/11/2015
 * .
 */
class DataTransferInfo {
    double speedMbps;
    double deltaTimeSegs;
    double deltaMBytes;
    long currentNanoTime;

    public DataTransferInfo(double speedMbps, double deltaTimeSegs, double deltaBytes, long currentNanoTime) {
        this.speedMbps = speedMbps;
        this.deltaTimeSegs = deltaTimeSegs;
        this.deltaMBytes = deltaBytes / (1024.0 * 1024);
        this.currentNanoTime = currentNanoTime;
    }

    @Override
    public String toString() {
        return String.format("%5.2f, %3.1f, %5.2f", speedMbps, deltaTimeSegs, deltaMBytes);
    }

    public String toStringDetailed() {
        return String.format("%5.9f, %3.9f, %5.9f", speedMbps, deltaTimeSegs, deltaMBytes);
    }
}
