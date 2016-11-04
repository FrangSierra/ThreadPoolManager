package com.frangsierra.threadmanager.executor;

/**
 * UI thread abstraction created to change the execution context from any thread to the UI thread.
 */
public interface MainThread {

    void post(final Runnable runnable);

    void postDelayed(final Runnable runnable, final long delayInMilli);
}