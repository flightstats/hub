package com.flightstats.hub.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Use Started as a static final in your class that you only want to run once.
 */
//todo - gfm - 2/10/14 - can this go away?
public class Started {

    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Sets the start status to true, if it isn't started yet.
     * @return true if this is already started
     */
    public synchronized boolean start() {
        if (started.get()) {
            return true;
        }
        started.set(true);
        return false;
    }

    public boolean isStarted() {
        return started.get();
    }
}
