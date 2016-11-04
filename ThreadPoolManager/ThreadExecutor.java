package com.frangsierra.threadmanager.executor;

import java.util.concurrent.Executor;

/**
 * Executor thread abstraction created to change the execution context from any thread from out ThreadExecutor.
 */
public interface ThreadExecutor extends Executor {

    void execute(Runnable runnable, String runnableName);

    void shutDown();
}
