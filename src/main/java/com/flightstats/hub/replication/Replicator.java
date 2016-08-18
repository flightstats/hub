package com.flightstats.hub.replication;

public interface Replicator {
    String REPLICATED = "replicated";
    String GLOBAL = "global";

    void stop();
}
