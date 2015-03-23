package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfig;

public interface ChannelReplicator {

    ChannelConfig getChannel();

    void exit();

    void stop();
}
