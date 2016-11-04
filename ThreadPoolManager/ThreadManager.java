package com.frangsierra.threadmanager.executor;

import android.util.Log;

/**
 * Module created to provide every dependency related with our execution service. Main
 * dependencies provided by this module are: ThreadExecutorImpl and MainThreadImpl.
 */
public final class ThreadManager {

    private static String TAG = "ThreadManager";

    public static String DEFAULT_TAG = "Runnable";

    private ThreadExecutorImpl mThreadExecutor;
    private MainThreadImpl mMainThreadImpl;
    private static ThreadManager sThreadManagerInstance;

    private ThreadManager() {
        mThreadExecutor = new ThreadExecutorImpl();
        mMainThreadImpl = new MainThreadImpl();
    }

    public ThreadExecutorImpl provideExecutor() {
        Log.i(TAG, "[provideExecutor] Providing Executor");
        return mThreadExecutor;
    }

    public MainThread provideMainThread() {
        Log.i(TAG, "[provideMainThread] Providing MainThread Executor");
        return mMainThreadImpl;
    }

    public synchronized static ThreadManager getThreadManagerInstance() {
        if (sThreadManagerInstance == null) {
            Log.i(TAG, "[getThreadManagerInstance] New Instance");
            sThreadManagerInstance = new ThreadManager();
        }
        return sThreadManagerInstance;
    }
}