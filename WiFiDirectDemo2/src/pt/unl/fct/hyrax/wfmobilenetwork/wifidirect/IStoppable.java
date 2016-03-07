package pt.unl.fct.hyrax.wfmobilenetwork.wifidirect;

/**
 * Created by DR & AT on 26/05/2015.
 * .
 */
public interface IStoppable extends Runnable{
    void start();
    void stopThread();
}
