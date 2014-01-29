package com.flightstats.datahub.cluster;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface Lockable {

    String getLockPath();

    long getAcquireTime();

    TimeUnit getAcquireUnit();

    /**
     * This will be called in the same thread
     */
    void runWithLock();

}
