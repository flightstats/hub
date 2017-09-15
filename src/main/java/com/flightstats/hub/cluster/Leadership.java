package com.flightstats.hub.cluster;

public interface Leadership {
    boolean hasLeadership();

    void close();

    void setLeadership(boolean leadership);
}
