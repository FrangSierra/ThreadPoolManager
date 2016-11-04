package com.frangsierra.threadmanager.executor;

/**
 * Callback task works as a Runnable but it implements a callback to check when the runnable has finished.
 */
public class CallbackTask implements Runnable {

    private static String TAG = "CallbackTask";

    private final Runnable mTask;
    private final String mName;
    private final int mRunnablePriority;
    private final RunnableCallback mCallback;

    public CallbackTask(Runnable task, String name, int runnablePriority, RunnableCallback runnableCallback) {
        this.mTask = task;
        this.mCallback = runnableCallback;
        this.mName = name;
        this.mRunnablePriority = runnablePriority;
    }

    public void run() {
        long startRunnable = System.currentTimeMillis();
        mTask.run();
        mCallback.onRunnableComplete(startRunnable, this.mName, this.mRunnablePriority);
    }

    public interface RunnableCallback{
        void onRunnableComplete(long runnableStartTime, String runnableName, int runnablePriority);
    }
}
