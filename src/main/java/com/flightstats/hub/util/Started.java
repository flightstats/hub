package com.flightstats.hub.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Use Started as a static final in your class that you only want to run once.
 */
public class Started {

    private final AtomicBoolean started = new AtomicBoolean(false);

    public synchronized boolean isStarted() {
        if (started.get()) {
            return true;
        }
        started.set(true);
        return false;
    }
}
