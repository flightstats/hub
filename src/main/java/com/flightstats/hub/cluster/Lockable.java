package com.flightstats.hub.cluster;

public interface Lockable {

    /**
     * This will be called in the same thread.
     */
    default void runWithLock() throws Exception {
        //todo - gfm - deprecated?
    }

    default void takeLeadership(Leadership leadership) throws Exception {
        //todo - gfm -
        //do nothing for now ...
    }

}
