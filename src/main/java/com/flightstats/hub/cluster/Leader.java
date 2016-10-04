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

    /**
     * This value should be between 0 and 1.
     * If the value is less than 1 (one), the current leader will keep leadership that percentage of the time.
     * For example, the default rate of 0.99 means that leadership will stay 99% of the time, and the leader
     * will abdicate 1% of the time.
     * If leadership is not rotated, the oldest running server will have all of the locks, and a disproportionate
     * amount of cluster's load.
     *
     * @return
     */
    default double keepLeadershipRate() {
        return 0.98;
    }

    default String getId() {
        return "";
    }
}