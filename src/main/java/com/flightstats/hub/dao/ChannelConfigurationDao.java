package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;

public interface ChannelConfigurationDao {
    ChannelConfiguration createChannel(ChannelConfiguration configuration);

    void updateChannel(ChannelConfiguration newConfig);

    void initialize();

    boolean channelExists(String channelName);

    ChannelConfiguration getChannelConfiguration(String channelName);

    Iterable<ChannelConfiguration> getChannels();

    void delete(String channelName);
}
