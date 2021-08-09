package com.flightstats.hub.cluster;

public interface Lockable {

    void takeLeadership(Leadership leadership);

}
