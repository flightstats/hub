package com.flightstats.hub.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

public interface Leader {
    /**
     * Called when this class is the Leader.
     * If this class loses leadership for any reason, the executing Thread will be interrupted with Future.cancel(true).
     * Implementers of this class should not modify the state of hasLeadership.
     *
     * @param hasLeadership - use this to properly exit loops which aren't cancelled by Future.cancel(true)
     */
    void takeLeadership(AtomicBoolean hasLeadership);

    /**
     * This value should be between 0 and 1.
     * If the value is less than 1 (one), the current leader will keep leadership that percentage of the time.
     * For example, the default rate of 0.75 means that leadership will stay 75% of the time, and the leader
     * will abdicate 25% of the time.
     *
     * @return
     */
    default double keepLeadershipRate() {
        return 0.75;
    }
}