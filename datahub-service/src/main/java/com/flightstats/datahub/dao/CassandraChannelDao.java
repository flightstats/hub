package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;

public class CassandraChannelDao implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);

    private final CassandraChannelsCollection channelsCollection;
    private final CassandraValueWriter cassandraValueWriter;
    private final CassandraValueReader cassandraValueReader;
    private final ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel;
    private final LastKeyFinder lastKeyFinder;
    private final DataHubKeyGenerator keyGenerator;
    private final TimeProvider timeProvider;

    @Inject
    public CassandraChannelDao(
            CassandraChannelsCollection channelsCollection,
            CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel,
            LastKeyFinder lastKeyFinder, DataHubKeyGenerator keyGenerator,
            TimeProvider timeProvider) {
        this.channelsCollection = channelsCollection;
        this.cassandraValueWriter = cassandraValueWriter;
        this.cassandraValueReader = cassandraValueReader;
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.lastKeyFinder = lastKeyFinder;
        this.keyGenerator = keyGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollection.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String name, Long ttlMillis) {
        logger.info("Creating channel name = " + name + ", with ttlMillis = " + ttlMillis);
        ChannelConfiguration configuration = channelsCollection.createChannel(name, ttlMillis);
        keyGenerator.seedChannel(name);
        return configuration;
    }

    @Override
    public void updateChannelMetadata(ChannelConfiguration newConfig) {
        channelsCollection.updateChannel(newConfig);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        logger.debug("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, data, timeProvider.getMillis());
        int ttlSeconds = getTtlSeconds(channelName);
        ValueInsertionResult result = cassandraValueWriter.write(channelName, value, ttlSeconds);
        DataHubKey insertedKey = result.getKey();
        setLastUpdateKey(channelName, insertedKey);
        if (insertedKey.isNewRow()) {
            channelsCollection.updateLatestRowKey(channelName, result.getRowKey());
        }
        return result;
    }

    private int getTtlSeconds(String channelName) {
        ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
        if (null == channelConfiguration) {
            return 0;
        }
        return (int) (channelConfiguration.getTtlMillis() / 1000);
    }

    @Override
    public void delete(String channelName, List<DataHubKey> keys) {
        cassandraValueWriter.delete(channelName, keys);
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
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        logger.debug("Fetching " + key.toString() + " from channel " + channelName);
        DataHubCompositeValue value = cassandraValueReader.read(channelName, key);
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
        return channelsCollection.getChannelConfiguration(channelName);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return channelsCollection.getChannels();
    }

    @Override
    public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
        try {
            DataHubKey latest = getLastUpdatedFromCache(channelName);
            if (latest == null) {
                latest = lastKeyFinder.queryForLatestKey(channelName);
                if (latest != null) {
                    lastUpdatedPerChannel.putIfAbsent(channelName, latest);
                }
            }
            return Optional.fromNullable(latest);
        } catch (HInvalidRequestException e) {
            throw maybePromoteToNoSuchChannel(e, channelName);
        }
    }

    private DataHubKey getLastUpdatedFromCache(String channelName) {
        return lastUpdatedPerChannel.get(channelName);
    }

    @Override
    public int countChannels() {
        return channelsCollection.countChannels();
    }
}
