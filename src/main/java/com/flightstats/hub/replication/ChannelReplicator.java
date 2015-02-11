package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfiguration;

public interface ChannelReplicator {

    ChannelConfiguration getChannel();

    void exit();

    void stop();
}
