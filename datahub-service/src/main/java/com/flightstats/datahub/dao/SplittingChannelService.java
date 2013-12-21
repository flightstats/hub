package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 *
 */
public class SplittingChannelService implements ChannelService {

    private final ChannelDao sequentialDao;
    private final ChannelDao timeSeriesDao;
    private final ChannelsCollectionDao channelsCollectionDao;
    private final ChannelDao missingDao = new ChannelDao() {
        @Override
        public void createChannel(ChannelConfiguration configuration) {
        }

        @Override
        public ValueInsertionResult insert(ChannelConfiguration configuration, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
            return null;
        }

        @Override
        public Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id) {
            return Optional.absent();
        }

        @Override
        public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
            return Optional.absent();
        }

    };

    @Inject
    public SplittingChannelService(@Sequential ChannelDao sequentialDao,
                                   @TimeSeries ChannelDao timeSeriesDao,
                                   ChannelsCollectionDao channelsCollectionDao) {
        this.sequentialDao = sequentialDao;
        this.timeSeriesDao = timeSeriesDao;
        this.channelsCollectionDao = channelsCollectionDao;
    }

    private ChannelDao getChannelDao(String channelName){
        ChannelConfiguration channelConfiguration = channelsCollectionDao.getChannelConfiguration(channelName);
        if (null == channelConfiguration) {
            return missingDao;
        }
        return getChannelDao(channelConfiguration);
    }

    private ChannelDao getChannelDao(ChannelConfiguration channelConfiguration) {
        if (channelConfiguration.isSequence()) {
            return sequentialDao;
        }
        return timeSeriesDao;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollectionDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        getChannelDao(configuration).createChannel(configuration);
        return channelsCollectionDao.createChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        ChannelConfiguration configuration = channelsCollectionDao.getChannelConfiguration(channelName);
        return getChannelDao(channelName).insert(configuration, contentType, contentLanguage, data);
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id) {
        return getChannelDao(channelName).getValue(channelName, id);
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
        return getChannelDao(channelName).findLastUpdatedKey(channelName);
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
