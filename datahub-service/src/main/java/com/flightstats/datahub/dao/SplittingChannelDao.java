package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * todo - gfm - 12/20/13 - it might be cleaner to pull out a separate interface for SplittingChannelDao.
 * direct calls to the channels collection could be removed from the ChannelDao interface
 */
public class SplittingChannelDao implements ChannelDao {

    private final ChannelDao sequentialDao;
    private final ChannelDao timeSeriesDao;
    private final ChannelsCollectionDao channelsCollectionDao;

    @Inject
    public SplittingChannelDao(@Sequential ChannelDao sequentialDao,
                               @TimeSeries ChannelDao timeSeriesDao,
                               ChannelsCollectionDao channelsCollectionDao) {
        this.sequentialDao = sequentialDao;
        this.timeSeriesDao = timeSeriesDao;
        this.channelsCollectionDao = channelsCollectionDao;
    }

    private ChannelDao getDao(String channelName){
        //todo - gfm - 12/20/13 - work on this next
        ChannelConfiguration channelConfiguration = channelsCollectionDao.getChannelConfiguration(channelName);
        /*if (channelConfiguration.isTimeSeries()) {
            return timeSeriesDao;
        }*/
        return sequentialDao;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollectionDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String channelName, Long ttlMillis) {
        return getDao(channelName).createChannel(channelName, ttlMillis);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        return getDao(channelName).insert(channelName, contentType, contentLanguage, data);
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        return getDao(channelName).getValue(channelName, key);
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id) {
        return getDao(channelName).getValue(channelName, id);
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
        return getDao(channelName).findLastUpdatedKey(channelName);
    }

    @Override
    public boolean isHealthy() {
        return sequentialDao.isHealthy() && timeSeriesDao.isHealthy();
    }

    @Override
    public void updateChannelMetadata(ChannelConfiguration newConfig) {
        getDao(newConfig.getName()).updateChannelMetadata(newConfig);
    }
}
