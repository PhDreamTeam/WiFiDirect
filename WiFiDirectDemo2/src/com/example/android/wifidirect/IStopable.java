package com.example.android.wifidirect;

/**
 * Created by DR & AT on 26/05/2015.
 *
 */
public interface IStopable extends Runnable{
    void start();
    void stopThread();
}
