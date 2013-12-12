package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class ChannelDaoImpl implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);

    private final ChannelsCollectionDao channelsCollectionDao;
    private final DataHubValueDao dataHubValueDao;
    private final ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel;
    private final TimeProvider timeProvider;
    private ChannelInsertionPublisher channelInsertionPublisher;

    @Inject
    public ChannelDaoImpl(
            ChannelsCollectionDao channelsCollectionDao,
            DataHubValueDao dataHubValueDao,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel,
            TimeProvider timeProvider,
            ChannelInsertionPublisher channelInsertionPublisher) {
        this.channelsCollectionDao = channelsCollectionDao;
        this.dataHubValueDao = dataHubValueDao;
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.timeProvider = timeProvider;
        this.channelInsertionPublisher = channelInsertionPublisher;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollectionDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String name, Long ttlMillis) {
        logger.info("Creating channel name = " + name + ", with ttlMillis = " + ttlMillis);
        ChannelConfiguration configuration = channelsCollectionDao.createChannel(name, ttlMillis);
        dataHubValueDao.initializeChannel(name);
        return configuration;
    }

    @Override
    public void updateChannelMetadata(ChannelConfiguration newConfig) {
        channelsCollectionDao.updateChannel(newConfig);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        logger.debug("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, data, timeProvider.getMillis());
        Optional<Integer> ttlSeconds = getTtlSeconds(channelName);
        ValueInsertionResult result = dataHubValueDao.write(channelName, value, ttlSeconds);
        DataHubKey insertedKey = result.getKey();
        setLastUpdateKey(channelName, insertedKey);
        /*if (insertedKey.isNewRow()) {
            channelsCollectionDao.updateLatestRowKey(channelName, result.getRowKey());
        }*/
        channelInsertionPublisher.publish(channelName, result);
        return result;
    }

    private Optional<Integer> getTtlSeconds(String channelName) {
        ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
        if (null == channelConfiguration) {
            return Optional.absent();
        }
        Long ttlMillis = channelConfiguration.getTtlMillis();
        return ttlMillis == null ? Optional.<Integer>absent() : Optional.of((int) (ttlMillis / 1000));
    }

    @Override
    public void delete(String channelName, List<DataHubKey> keys) {
        dataHubValueDao.delete(channelName, keys);
    }

    @Override
    public void setLastUpdateKey(String channelName, DataHubKey lastUpdateKey) {
        lastUpdatedPerChannel.put(channelName, lastUpdateKey);
    }

    @Override
    public void deleteLastUpdateKey(String channelName) {
        lastUpdatedPerChannel.remove(channelName);
    }

    @Override
    public boolean isHealthy() {
        return channelsCollectionDao.isHealthy();
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        logger.debug("Fetching " + key.toString() + " from channel " + channelName);
        DataHubCompositeValue value = dataHubValueDao.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        Optional<DataHubKey> previous = key.getPrevious();
        Optional<DataHubKey> next = key.getNext();
        Optional<DataHubKey> lastUpdatedKey = findLastUpdatedKey(channelName);
        if (lastUpdatedKey.isPresent()) {
            if (lastUpdatedKey.get().getSequence() == key.getSequence()) {
                next = Optional.absent();
            }
        }

        return Optional.of(new LinkedDataHubCompositeValue(value, previous, next));
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

        return Optional.fromNullable(getLastUpdatedFromCache(channelName));
    }

    private DataHubKey getLastUpdatedFromCache(String channelName) {
        return lastUpdatedPerChannel.get(channelName);
    }

}
