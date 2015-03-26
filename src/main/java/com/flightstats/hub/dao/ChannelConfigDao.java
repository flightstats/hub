package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;

public interface ChannelConfigDao {
    ChannelConfig createChannel(ChannelConfig config);

    void updateChannel(ChannelConfig newConfig);

    boolean channelExists(String name);

    ChannelConfig getChannelConfig(String name);

    Iterable<ChannelConfig> getChannels();

    void delete(String name);
}
