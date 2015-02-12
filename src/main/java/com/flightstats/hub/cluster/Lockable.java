package com.flightstats.hub.cluster;

public interface Lockable {

    /**
     * This will be called in the same thread.
     */
    void runWithLock() throws Exception;

}
