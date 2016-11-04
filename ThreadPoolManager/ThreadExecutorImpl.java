package com.frangsierra.threadmanager.executor;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutorImpl implements ThreadExecutor {

    private static String TAG = "ThreadExecutorImpl";
    public static final int MAX_SINGLE_THREADPOOL_SIZE = 7;

    private final int MAX_CONCURRENT_THREADS = 4;
    private final int DEFAULT_PRIORITY = Thread.NORM_PRIORITY;
    private final String DEFAULT_THREAD_NAME = "CameraThread";

    private int mCurrentExecutors = 0;

    private long mSubmittedProcessingTasks;
    private long mCompletedProcessingTasks;
    private long mNotCompletedProcessingTasks;

    private final TaskHashMap SubmittedTasksMap = new TaskHashMap();
    private ThreadPoolExecutor mThreadPoolExecutor;

    private ExecutorService mLowPrioritySingleThread;
    private ExecutorService mMediumPrioritySingleThread;
    private ExecutorService mHighPrioritySingleThread;

    public ThreadExecutorImpl() {
        Log.i(TAG, "[ThreadExecutorImpl] Initializing ThreadExecutorImpl");
        Log.i(TAG, "[ThreadExecutorImpl] current cores: " + Runtime.getRuntime().availableProcessors());
        this.mThreadPoolExecutor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool((MAX_CONCURRENT_THREADS - mCurrentExecutors),
                        new ThreadFactoryBuilder()
                                .setPriority(Thread.NORM_PRIORITY).build());
    }

    public void executeSingleThread(Runnable runnable) {
        executeSingleThread(runnable, DEFAULT_PRIORITY, DEFAULT_THREAD_NAME, false);
    }

    public void executeSingleThread(Runnable runnable, int priority) {
        executeSingleThread(runnable, priority, DEFAULT_THREAD_NAME, false);
    }

    public void executeSingleThread(Runnable runnable, int priority, String threadName) {
        executeSingleThread(runnable, priority, threadName, false);
    }

    /**
     * This method recieve a runnable and their custom parameters to generate a Thread associate to
     * a ThreadFactory which will implement all that parameters. When the method is called, first
     * it check the priority and depending of that, we will try to generate a new Thread based on that
     * priority. If that thread is currently available and working, we will just add to their queue the
     * new runnable.
     *
     * @param runnable current Runnable.
     * @param priority Thread priority. Higher priority threads are executed in preference to lower priority threads.
     * @param threadName Name of the thread.
     * @param isDaemon boolean to set this thread as a Daemon
     */
    public void executeSingleThread(Runnable runnable,@NonNull int priority,@NonNull String threadName,@NonNull boolean isDaemon) {
        Log.i(TAG, "[executeSingleThread] new task has been submited \n " +
                "Thread name: " + threadName + " \n " +
                "Priority: " + priority + " \n " +
                "¿Daemon? : " + isDaemon);

        ExecutorService currentExecutor = null;
        switch (priority) {

            case (Thread.MIN_PRIORITY):
                if (mLowPrioritySingleThread == null || mLowPrioritySingleThread.isTerminated() || mLowPrioritySingleThread.isShutdown()) {
                    mCurrentExecutors++;
                    //Reallocate Threads from our processing thread
                    reallocateThreads();
                    //Initialize the low priority thread.
                    mLowPrioritySingleThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                            .setPriority(Thread.MIN_PRIORITY)
                            .setNamePrefix(threadName)
                            .setDaemon(isDaemon)
                            .build());
                    Log.v(TAG, "[executeSingleThread] Low priority single thread initialize");
                }
                currentExecutor = mLowPrioritySingleThread;
                break;

            case (Thread.NORM_PRIORITY):
                if (mMediumPrioritySingleThread == null || mMediumPrioritySingleThread.isTerminated() || mMediumPrioritySingleThread.isShutdown()) {
                    mCurrentExecutors++;
                    //Reallocate Threads from our Processing thread
                    reallocateThreads();
                    //Initialize the medium priority thread.
                    mMediumPrioritySingleThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                            .setPriority(Thread.NORM_PRIORITY)
                            .setNamePrefix(threadName)
                            .setDaemon(isDaemon)
                            .build());
                    Log.v(TAG, "[executeSingleThread] Medium priority single thread initialize");
                }
                currentExecutor = mMediumPrioritySingleThread;
                break;

            case (Thread.MAX_PRIORITY):
                if (mHighPrioritySingleThread == null || mHighPrioritySingleThread.isTerminated() || mHighPrioritySingleThread.isShutdown()) {
                    mCurrentExecutors++;
                    //Reallocate Threads from our Processing thread
                    reallocateThreads();
                    //Initialize the high priority thread.
                    mHighPrioritySingleThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                            .setPriority(Thread.MAX_PRIORITY)
                            .setNamePrefix(threadName)
                            .setDaemon(isDaemon)
                            .build());
                    Log.v(TAG, "[executeSingleThread] High priority single thread initialize");
                }
                currentExecutor = mHighPrioritySingleThread;
                break;
        }

        if (currentExecutor != null) {
            //Plus one to the task of this current priority
            SubmittedTasksMap.put(priority, SubmittedTasksMap.get(priority) + 1);
            //Run the thread
            runSingleThread(currentExecutor, runnable, threadName, priority);
        }
    }

    /**
     * Execute a Runnable in a specific priority single thread.
     * Single threads uses a single worker thread operating off an unbounded queue, and uses the
     * provided ThreadFactory to create a new thread when needed
     * @param executor Executor where the runnable is going to be executed
     * @param runnable Current runnable
     * @param threadName Thread name. It will be showed in logs coming from the current thread.
     * @param priority  Thread priority. Higher priority threads are executed in preference to lower priority threads.
     */
    private void runSingleThread(ExecutorService executor, Runnable runnable, String threadName, int priority) {
        executor.execute(new CallbackTask(runnable, threadName, priority, new CallbackTask.RunnableCallback() {
            @Override
            public void onRunnableComplete(long RunnableStartTime, String runnableName, int priority) {
                SubmittedTasksMap.put(priority, (SubmittedTasksMap.get(priority).intValue() - 1));
                Log.i(TAG, "[executeSingleThread] [onRunnableComplete] Runnable " + runnableName + " complete in " + (System.currentTimeMillis() - RunnableStartTime) + "ms");
                Log.i(TAG, "[executeSingleThread] [onRunnableComplete] Current Single Queue " + SubmittedTasksMap.getTotalSubmittedTask());
                tryToShutDownThreads(priority);
            }
        }));
    }

    /**
     * This method reallocate the {@link ThreadExecutorImpl#MAX_CONCURRENT_THREADS} current available
     * threads to the {@link ThreadExecutorImpl#mThreadPoolExecutor}, which is responsible of the fast processing tasks for the
     * application
     */
    private synchronized void reallocateThreads() {
        mThreadPoolExecutor.setCorePoolSize(MAX_CONCURRENT_THREADS - mCurrentExecutors);
        Log.i(TAG, "[reallocateThreads] new processing background core: " + mThreadPoolExecutor.getCorePoolSize());
    }

    /**
     * this method check the pending tasks for the current priority thread and if the thread
     * doesn't have more pendent task, it will be close and the cores will be reallocated to the
     * process thread.
     * @param priority Thread priority.
     */
    private void tryToShutDownThreads(int priority) {
        ExecutorService currentExecutor = null;
        switch (priority) {
            case (Thread.MIN_PRIORITY):
                if (mLowPrioritySingleThread != null && SubmittedTasksMap.get(priority).intValue() == 0 && !mLowPrioritySingleThread.isShutdown()) {
                    Log.i(TAG, "[tryToShutDownThreads] Calling shutdown for low priority pool");
                    currentExecutor = mLowPrioritySingleThread;
                }
                break;
            case (Thread.NORM_PRIORITY):
                if (mMediumPrioritySingleThread != null && SubmittedTasksMap.get(priority).intValue() == 0 && !mMediumPrioritySingleThread.isShutdown()) {
                    Log.i(TAG, "[tryToShutDownThreads] Calling shutdown for medium priority pool");
                    currentExecutor = mMediumPrioritySingleThread;
                }
                break;
            case (Thread.MAX_PRIORITY):
                if (mHighPrioritySingleThread != null && SubmittedTasksMap.get(priority).intValue() == 0 && !mHighPrioritySingleThread.isShutdown()) {
                    Log.i(TAG, "[tryToShutDownThreads] Calling shutdown for high priority pool");
                    currentExecutor = mHighPrioritySingleThread;
                }
                break;
        }
        if (currentExecutor != null) {
            currentExecutor.shutdown();
            mCurrentExecutors--;
            reallocateThreads();
        }
    }

    /**
     * This method check if our current tasks submitted through all our SingleThreads  surpass the
     * {@link ThreadExecutorImpl#MAX_SINGLE_THREADPOOL_SIZE} value
     * @return true or false depending of our current tasks and the max value that we set upside
     *
     * It is usefull if we are calling a lot of expensive calls queued in our SingleThread. We need to
     * limit them.
     */
    public boolean getSingleThreadExecutorQueue() {
        Log.i(TAG, "[getSingleThreadExecutorQueue] Single Thread executor queue " + SubmittedTasksMap.getTotalSubmittedTask());
        return SubmittedTasksMap.getTotalSubmittedTask() >= MAX_SINGLE_THREADPOOL_SIZE;
    }

    @Override
    public void execute(Runnable runnable) {
        if (mThreadPoolExecutor == null || mThreadPoolExecutor.isTerminated() || mThreadPoolExecutor.isShutdown()) {
            recreateMainBackgroundPool();
            Log.v(TAG, "[execute] ThreadPool initialize with " + mThreadPoolExecutor.getCorePoolSize() + " cores");
        }
        execute(runnable, ThreadManager.DEFAULT_TAG);
    }

    /**
     * Execute a Thread in the main background thread. This method may be used with short tasks.
     * The Max value for this thread is totally dynamic, it various programatically depending of the
     * max threads that we want to use in our app and the current executers working at the same time.
     * @param runnable Current runnable.
     * @param runnableName Runnable name.
     */
    @Override
    public void execute(Runnable runnable, String runnableName) {
        try {
            if (runnable == null) {
                Log.e(TAG, "[execute] Runnable to execute cannot be null");
                return;
            }
            Log.i(TAG, "[execute] Executing new Thread");

            this.mThreadPoolExecutor.execute(new CallbackTask(runnable, runnableName, Thread.NORM_PRIORITY, new CallbackTask.RunnableCallback() {

                @Override
                public void onRunnableComplete(long runnableStartTime, String runnableName, int runnablePriority) {
                    mSubmittedProcessingTasks = mThreadPoolExecutor.getTaskCount();
                    mCompletedProcessingTasks = mThreadPoolExecutor.getCompletedTaskCount();
                    mNotCompletedProcessingTasks = mSubmittedProcessingTasks - mCompletedProcessingTasks; // approximate

                    Log.i(TAG, "[execute] [onRunnableComplete] Runnable " + runnableName + " complete in " + (System.currentTimeMillis() - runnableStartTime) + "ms");
                    Log.i(TAG, "[execute] [onRunnableComplete] Current threads working " + mNotCompletedProcessingTasks);
                }

            }));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "[execute] Error, shutDown the Executor");
            //ShutdownNow attempts to stop all actively executing tasks, halts the processing of waiting tasks
            mThreadPoolExecutor.shutdownNow();
        }
    }

    /**
     * Call the shutDown method for the mainBackgroundThread. When shutDown is called it initialize an
     * orderly shutdown in which previously submitted tasks are executed,
     * but no new tasks will be accepted. Invocation has no additional effect if already shut down.
     */
    @Override
    public void shutDown() {
        try {
            Log.i(TAG, "[shutDown] Calling shutDown for processing Thread");
            this.mThreadPoolExecutor.shutdown();

            // Wait for everything to finish.
            while (!mThreadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                mSubmittedProcessingTasks = mThreadPoolExecutor.getTaskCount();
                mCompletedProcessingTasks = mThreadPoolExecutor.getCompletedTaskCount();
                mNotCompletedProcessingTasks = mSubmittedProcessingTasks - mCompletedProcessingTasks; // approximate
                Log.i(TAG, "[shutDown] Awaiting completion of threads. Current threads working " + mNotCompletedProcessingTasks);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "[shutDown] Error when calling shutdown");
        }
    }

    public void recreateMainBackgroundPool() {
        Log.i(TAG, "[recreateMainBackgroundPool]");
        this.mThreadPoolExecutor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool((MAX_CONCURRENT_THREADS - mCurrentExecutors),
                        new ThreadFactoryBuilder()
                                .setPriority(Thread.NORM_PRIORITY).build());
    }
}