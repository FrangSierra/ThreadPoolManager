package com.frangsierra.threadmanager.executor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * MainThread implementation based on a Handler instantiated over the main looper obtained from
 * Looper class.
 *
 */
public class MainThreadImpl implements MainThread {

    private static String TAG = "MainThreadImpl";
    private Handler mHandler;

    public MainThreadImpl(){
        Log.i(TAG, "[MainThreadImpl] Creating MainThreadImpl instance");
        this.mHandler = new Handler(Looper.getMainLooper());
    }

    public void post(Runnable runnable) {
        Log.i(TAG, "[post] Adding new thread to handle queue");
        mHandler.post(runnable);
    }

    @Override
    public void postDelayed(Runnable runnable, long delayInMilli) {
        Log.i(TAG, "[postDelayed] New post Delayed Thead, executing in " + delayInMilli + " milliseconds");
        mHandler.postDelayed(runnable,delayInMilli);
    }

}