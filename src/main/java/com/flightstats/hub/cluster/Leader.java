package com.flightstats.hub.cluster;

public interface Leader
{
    /**
     * Called when this class is the Leader.
     * If this class loses leadership for any reason, the executing Thread will be interrupted.
     */
    void takeLeadership();

}