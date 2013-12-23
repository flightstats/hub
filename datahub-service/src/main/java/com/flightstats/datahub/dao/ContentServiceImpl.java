package com.flightstats.datahub.dao;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

public class ContentServiceImpl implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final ContentDao contentDao;
    private final ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel;
    private final TimeProvider timeProvider;
    private ChannelInsertionPublisher channelInsertionPublisher;
    private final MetricsTimer metricsTimer;

    @Inject
    public ContentServiceImpl(
            ContentDao contentDao,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel,
            TimeProvider timeProvider,
            ChannelInsertionPublisher channelInsertionPublisher,
            MetricsTimer metricsTimer) {
        this.contentDao = contentDao;
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.timeProvider = timeProvider;
        this.channelInsertionPublisher = channelInsertionPublisher;
        this.metricsTimer = metricsTimer;
    }

    @Override
    public void createChannel(ChannelConfiguration configuration) {
        logger.info("Creating channel " + configuration);
        contentDao.initializeChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(ChannelConfiguration configuration, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        String channelName = configuration.getName();
        logger.debug("inserting {} bytes into channel {} ", data.length, channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, data, timeProvider.getMillis());
        Optional<Integer> ttlSeconds = getTtlSeconds(configuration);
        ValueInsertionResult result = contentDao.write(channelName, value, ttlSeconds);
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

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id) {
        Optional<DataHubKey> keyOptional = contentDao.getKey(id);
        if (!keyOptional.isPresent()) {
            return Optional.absent();
        }
        DataHubKey key = keyOptional.get();
        logger.debug("fetching {} from channel {} ", key.toString(), channelName);
        DataHubCompositeValue value = contentDao.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        Optional<DataHubKey> previous = key.getPrevious();
        Optional<DataHubKey> next = key.getNext();
        if (next.isPresent()) {
            Optional<DataHubKey> lastUpdatedKey = findLastUpdatedKey(channelName);
            if (lastUpdatedKey.isPresent()) {
                if (lastUpdatedKey.get().equals(key)) {
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

    @Override
    public Optional<Iterable<DataHubKey>> getKeys(String channelName, DateTime dateTime) {
        return contentDao.getKeys(channelName, dateTime);
    }

    private void setLastUpdateKey(final String channelName, final DataHubKey lastUpdateKey) {
        metricsTimer.time("hazelcast.setLastUpdated", new TimedCallback<Object>() {
            @Override
            public Object call() {
                lastUpdatedPerChannel.put(channelName, lastUpdateKey);
                return null;
            }
        });
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
