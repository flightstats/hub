package com.flightstats.hub.replication;

public interface Replicator {
    String REPLICATED = "replicated";

    void notifyWatchers();
}
