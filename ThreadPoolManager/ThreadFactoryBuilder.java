package com.frangsierra.threadmanager.executor;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadFactoryBuilder {
    /**
     * The default thread factory does not allow you to specify custom thread names.
     * All the threads that it creates are named as pool-1-thread-1, pool-2-thread-3 and so on.
     */
    private String mNamePrefix = null;
    /**
     * The default thread factory only produces user threads. It cannot produce daemon threads.
     * User threads prevent JVM from exiting while daemon threads do not, meaning, if no user
     * thread is running, JVM will exit irrespective of daemon thread(s) running. For implementing
     * background services, daemon threads are preferred. For eg. Garbage Collector runs as a daemon thread.
     */
    private boolean mDaemon = false;

    /**
     * The default thread factory creates threads with normal priority.
     * But sometimes we may need to create prioritized threads. Higher priority threads are
     * executed in preference to lower priority threads.
     */
    private int mPriority = Thread.NORM_PRIORITY;

    /**
     * When an uncaught runtime exception occurs in a thread when executing a task, by default,
     * the thread is terminated and the exception is logged to the console.
     * This default behavior may not be always appropriate. For eg. You may need to log the
     * exception to a log file or update a database to record the failing task. You can achieve
     * this by setting a custom uncaught exception handler on the thread created, in the custom
     * thread factory, to handle the uncaught exceptions as desired.
     */
    private UncaughtExceptionHandler mUncaughtExceptionHandler = null;

    public ThreadFactoryBuilder setNamePrefix(String mNamePrefix) {
        if (mNamePrefix == null) {
            throw new NullPointerException();
        }
        this.mNamePrefix = mNamePrefix;
        return this;
    }

    public ThreadFactoryBuilder setDaemon(boolean mDaemon) {
        this.mDaemon = mDaemon;
        return this;
    }

    public ThreadFactoryBuilder setPriority(int priority) {
        if (priority < Thread.MIN_PRIORITY) {
            throw new IllegalArgumentException(String.format(
                    "Thread priority (%s) must be >= %s", priority,
                    Thread.MIN_PRIORITY));
        }

        if (priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(String.format(
                    "Thread priority (%s) must be <= %s", priority,
                    Thread.MAX_PRIORITY));
        }

        this.mPriority = priority;
        return this;
    }

    public ThreadFactoryBuilder setUncaughtExceptionHandler(
            UncaughtExceptionHandler uncaughtExceptionHandler) {
        if (null == uncaughtExceptionHandler) {
            throw new NullPointerException(
                    "UncaughtExceptionHandler cannot be null");
        }
        this.mUncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    public ThreadFactory build() {
        return build(this);
    }

    private static ThreadFactory build(ThreadFactoryBuilder builder) {
        final String namePrefix = builder.mNamePrefix;
        final Boolean daemon = builder.mDaemon;
        final Integer priority = builder.mPriority;
        final UncaughtExceptionHandler uncaughtExceptionHandler = builder.mUncaughtExceptionHandler;
        final ThreadFactory backingThreadFactory =  Executors.defaultThreadFactory();

        final AtomicLong count = new AtomicLong(0);

        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = backingThreadFactory.newThread(runnable);
                if (namePrefix != null) {
                    thread.setName(namePrefix + "-" + count.getAndIncrement());
                }
                if (daemon != null) {
                    thread.setDaemon(daemon);
                }
                if (priority != null) {
                    thread.setPriority(priority);
                }
                if (uncaughtExceptionHandler != null) {
                    thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                }
                return thread;
            }
        };
    }
}