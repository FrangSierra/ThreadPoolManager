package com.frangsierra.threadmanager.executor;

import java.util.HashMap;
import java.util.Iterator;

class TaskHashMap extends HashMap<Integer, Integer> {
    public TaskHashMap() {
        this.put(Thread.MIN_PRIORITY, 0);
        this.put(Thread.NORM_PRIORITY, 0);
        this.put(Thread.MAX_PRIORITY, 0);
    }

    public int getTotalSubmittedTask() {
        int totalTasks = 0;
        Iterator mapIterator = this.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Entry entry = (Entry) mapIterator.next();
            totalTasks += (int) entry.getValue();
        }
        return totalTasks;
    }
}
