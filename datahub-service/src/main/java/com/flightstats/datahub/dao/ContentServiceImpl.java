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

//todo - gfm - 12/24/13 - wondering about the remaining value of this class
//seems like the common functionality could move up into ChannelService, and some lower level functionality
//could move into the DAOs
public class ContentServiceImpl implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final ContentDao contentDao;
    private final ConcurrentMap<String, ContentKey> lastUpdatedPerChannel;
    private final TimeProvider timeProvider;
    private ChannelInsertionPublisher channelInsertionPublisher;
    private final MetricsTimer metricsTimer;

    @Inject
    public ContentServiceImpl(
            ContentDao contentDao,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, ContentKey> lastUpdatedPerChannel,
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
        Content value = new Content(contentType, contentLanguage, data, timeProvider.getMillis());
        Optional<Integer> ttlSeconds = getTtlSeconds(configuration);
        ValueInsertionResult result = contentDao.write(channelName, value, ttlSeconds);
        ContentKey insertedKey = result.getKey();
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
    public Optional<LinkedContent> getValue(String channelName, String id) {
        Optional<ContentKey> keyOptional = contentDao.getKey(id);
        if (!keyOptional.isPresent()) {
            return Optional.absent();
        }
        ContentKey key = keyOptional.get();
        logger.debug("fetching {} from channel {} ", key.toString(), channelName);
        Content value = contentDao.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        Optional<ContentKey> previous = key.getPrevious();
        Optional<ContentKey> next = key.getNext();
        if (next.isPresent()) {
            Optional<ContentKey> lastUpdatedKey = findLastUpdatedKey(channelName);
            if (lastUpdatedKey.isPresent()) {
                if (lastUpdatedKey.get().equals(key)) {
                    next = Optional.absent();
                }
            }
        }

        return Optional.of(new LinkedContent(value, previous, next));
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        return Optional.fromNullable(getLastUpdatedFromCache(channelName));
    }

    @Override
    public Iterable<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return contentDao.getKeys(channelName, dateTime);
    }

    private void setLastUpdateKey(final String channelName, final ContentKey lastUpdateKey) {
        //todo - gfm - 12/24/13 - this is relatively slow with hazelcast and high throughput
        //wondering if this is needed for time series
        metricsTimer.time("hazelcast.setLastUpdated", new TimedCallback<Object>() {
            @Override
            public Object call() {
                lastUpdatedPerChannel.put(channelName, lastUpdateKey);
                return null;
            }
        });
    }

    private ContentKey getLastUpdatedFromCache(final String channelName) {
        return metricsTimer.time("hazelcast.getLastUpdated", new TimedCallback<ContentKey>() {
            @Override
            public ContentKey call() {
                return lastUpdatedPerChannel.get(channelName);
            }
        });
    }

}
