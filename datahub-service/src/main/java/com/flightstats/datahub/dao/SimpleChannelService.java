package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 *
 */
public class SimpleChannelService implements ChannelService {

    private final ChannelDao channelDao;
    private final ChannelsCollectionDao channelsCollectionDao;

    @Inject
    public SimpleChannelService(ChannelDao channelDao,
                                ChannelsCollectionDao channelsCollectionDao) {
        this.channelDao = channelDao;
        this.channelsCollectionDao = channelsCollectionDao;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollectionDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        channelDao.createChannel(configuration);
        return channelsCollectionDao.createChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        return channelDao.insert(channelsCollectionDao.getChannelConfiguration(channelName), contentType, contentLanguage, data);
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id) {
        Optional<DataHubKey> key = SequenceDataHubKey.fromString(id);
        return channelDao.getValue(channelName, key.get());
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return channelsCollectionDao.getChannelConfiguration(channelName);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return channelsCollectionDao.getChannels();
    }

    @Override
    public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
        return channelDao.findLastUpdatedKey(channelName);
    }

    @Override
    public boolean isHealthy() {
        return channelsCollectionDao.isHealthy();
    }

    @Override
    public void updateChannelMetadata(ChannelConfiguration newConfig) {
        channelsCollectionDao.updateChannel(newConfig);
    }
}
