package com.flightstats.hub.replication;

public interface Replicator {
    String REPLICATED = "replicated";
    String GLOBAL = "global";
    String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";

    void stop();
}
