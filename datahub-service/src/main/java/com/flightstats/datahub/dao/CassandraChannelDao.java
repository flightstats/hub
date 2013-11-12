package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

    @Inject
    public CassandraChannelDao(
            CassandraChannelsCollection channelsCollection,
            CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel,
            LastKeyFinder lastKeyFinder, DataHubKeyGenerator keyGenerator) {
        this.channelsCollection = channelsCollection;
        this.cassandraValueWriter = cassandraValueWriter;
        this.cassandraValueReader = cassandraValueReader;
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.lastKeyFinder = lastKeyFinder;
        this.keyGenerator = keyGenerator;
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
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, data);
        int ttlSeconds = getTtlSeconds(channelName);
        ValueInsertionResult result = cassandraValueWriter.write(channelName, value, ttlSeconds);
        DataHubKey insertedKey = result.getKey();
        setLastUpdateKey(channelName, insertedKey);
        updateFirstKey(channelName, insertedKey);
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

    private void updateFirstKey(String channelName, DataHubKey newLatestKey) {
        if (!findFirstUpdateKey(channelName).isPresent()) {
            setFirstKey(channelName, newLatestKey);
        }
    }

    @Override
    public void delete(String channelName, List<DataHubKey> keys) {
        cassandraValueWriter.delete(channelName, keys);
    }

    @Override
    public Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime) {
        throw new UnsupportedOperationException("This is not currently supported");
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
    public void setFirstKey(String channelName, DataHubKey result) {
        channelsCollection.updateFirstKey(channelName, result);
    }

    @Override
    public void deleteFirstKey(String channelName) {
        Optional<DataHubKey> firstId = findFirstUpdateKey(channelName);
        if (firstId.isPresent()) {
            channelsCollection.deleteFirstKey(channelName);
        }
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        logger.debug("Fetching " + key.toString() + " from channel " + channelName);
        DataHubCompositeValue value = cassandraValueReader.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        Optional<DataHubKey> previous = key.getPrevious();
        //todo - gfm - 11/4/13 - this may need some work
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
    public Optional<DataHubKey> findFirstUpdateKey(String channelName) {
        try {
            DataHubKey firstKey = channelsCollection.getFirstKey(channelName);
            return Optional.fromNullable(firstKey);
        } catch (HInvalidRequestException e) {
            throw maybePromoteToNoSuchChannel(e, channelName);
        }
    }

    @Override
    public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
        try {
            DataHubKey latest = getLastUpdatedFromCache(channelName);
            if (latest == null) {
                latest = lastKeyFinder.queryForLatestKey(channelName);
                if (latest != null) {
                    setLastUpdateKey(channelName, latest);
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
