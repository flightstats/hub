package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;

import java.util.Collection;

public interface ChannelConfigDao {
    ChannelConfig createChannel(ChannelConfig config);

    void updateChannel(ChannelConfig newConfig);

    boolean channelExists(String name);

    ChannelConfig getChannelConfig(String name);

    ChannelConfig getCachedChannelConfig(String name);

    Collection<ChannelConfig> getChannels(boolean useCache);

    void delete(String name);

    default boolean refresh() {
        return false;
    }
}
