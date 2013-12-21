package com.flightstats.datahub.dao;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

public class ChannelDaoImpl implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDaoImpl.class);

    private final DataHubValueDao dataHubValueDao;
    //todo - gfm - 12/20/13 - pull out this into it's own class, which will also remove the need for metricsTimer
    private final ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel;
    private final TimeProvider timeProvider;
    private ChannelInsertionPublisher channelInsertionPublisher;
    private final MetricsTimer metricsTimer;

    @Inject
    public ChannelDaoImpl(
            DataHubValueDao dataHubValueDao,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel,
            TimeProvider timeProvider,
            ChannelInsertionPublisher channelInsertionPublisher,
            MetricsTimer metricsTimer) {
        this.dataHubValueDao = dataHubValueDao;
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.timeProvider = timeProvider;
        this.channelInsertionPublisher = channelInsertionPublisher;
        this.metricsTimer = metricsTimer;
    }

    @Override
    public void createChannel(ChannelConfiguration configuration) {
        logger.info("Creating channel " + configuration);
        dataHubValueDao.initializeChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(ChannelConfiguration configuration, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        String channelName = configuration.getName();
        logger.debug("inserting {} bytes into channel {} ", data.length, channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, data, timeProvider.getMillis());
        Optional<Integer> ttlSeconds = getTtlSeconds(configuration);
        ValueInsertionResult result = dataHubValueDao.write(channelName, value, ttlSeconds);
        DataHubKey insertedKey = result.getKey();
        setLastUpdateKey(channelName, insertedKey);
        channelInsertionPublisher.publish(channelName, result);
        return result;
    }

    private Optional<Integer> getTtlSeconds(ChannelConfiguration channelConfiguration) {
        if (null == channelConfiguration) {
            return Optional.absent();
        }
        Long ttlMillis = channelConfiguration.getTtlMillis();
        return ttlMillis == null ? Optional.<Integer>absent() : Optional.of((int) (ttlMillis / 1000));
    }

    public void setLastUpdateKey(final String channelName, final DataHubKey lastUpdateKey) {
        metricsTimer.time("hazelcast.setLastUpdated", new TimedCallback<Object>() {
            @Override
            public Object call() {
                lastUpdatedPerChannel.put(channelName, lastUpdateKey);
                return null;
            }
        });
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        logger.debug("fetching {} from channel {} ", key.toString(), channelName);
        DataHubCompositeValue value = dataHubValueDao.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        Optional<DataHubKey> previous = key.getPrevious();
        Optional<DataHubKey> next = key.getNext();
        if (next.isPresent()) {
            Optional<DataHubKey> lastUpdatedKey = findLastUpdatedKey(channelName);
            if (lastUpdatedKey.isPresent()) {
                if (lastUpdatedKey.get().getSequence() == key.getSequence()) {
                    next = Optional.absent();
                }
            }
        }

        return Optional.of(new LinkedDataHubCompositeValue(value, previous, next));
    }

    @Override
    public Optional<DataHubKey> findLastUpdatedKey(String channelName) {

        return Optional.fromNullable(getLastUpdatedFromCache(channelName));
    }

    private DataHubKey getLastUpdatedFromCache(final String channelName) {
        return metricsTimer.time("hazelcast.getLastUpdated", new TimedCallback<DataHubKey>() {
            @Override
            public DataHubKey call() {
                return lastUpdatedPerChannel.get(channelName);
            }
        });
    }

}
