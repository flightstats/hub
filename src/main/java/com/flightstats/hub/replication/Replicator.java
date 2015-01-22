package com.flightstats.hub.replication;

public interface Replicator {
    V1ChannelReplicator getChannelReplicator(String channel);

    void notifyWatchers();
}
