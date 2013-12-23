package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;

/**
 *
 */
public interface ChannelMetadataDao {
    ChannelConfiguration createChannel(ChannelConfiguration configuration);

    void updateChannel(ChannelConfiguration newConfig);

    boolean isHealthy();

    void initializeMetadata();

    boolean channelExists(String channelName);

    ChannelConfiguration getChannelConfiguration(String channelName);

    Iterable<ChannelConfiguration> getChannels();
}
