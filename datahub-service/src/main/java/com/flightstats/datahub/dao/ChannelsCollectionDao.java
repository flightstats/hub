package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;

/**
 *
 */
public interface ChannelsCollectionDao {
    ChannelConfiguration createChannel(String name, Long ttlMillis);

    void updateChannel(ChannelConfiguration newConfig);

    int countChannels();

    void initializeMetadata();

    boolean channelExists(String channelName);

    ChannelConfiguration getChannelConfiguration(String channelName);

    Iterable<ChannelConfiguration> getChannels();
}
