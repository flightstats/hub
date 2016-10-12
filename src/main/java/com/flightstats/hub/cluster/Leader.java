package com.flightstats.hub.cluster;

public interface Leader {
    /**
     * Called when this class is the Leader.
     * If this class loses leadership for any reason, the executing Thread will be interrupted with Future.cancel(true).
     * Implementers of this class should not modify the state of hasLeadership.
     *
     * @param leadership - use this to properly exit loops which aren't cancelled by Future.cancel(true)
     */
    void takeLeadership(Leadership leadership);

    default String getId() {
        return "";
    }
}