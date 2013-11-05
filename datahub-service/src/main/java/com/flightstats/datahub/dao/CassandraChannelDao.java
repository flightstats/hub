package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;
import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;

//todo - gfm - 11/4/13 - make sure this is using the new sequence key correctly
public class CassandraChannelDao implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);

    private final CassandraChannelsCollection channelsCollection;
    private final CassandraValueWriter cassandraValueWriter;
    private final CassandraValueReader cassandraValueReader;
    private final DataHubKeyRenderer keyRenderer;
    private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel;
    final LastKeyFinder lastKeyFinder;

    @Inject
    public CassandraChannelDao(
            CassandraChannelsCollection channelsCollection,
            CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader,
            DataHubKeyRenderer keyRenderer, RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy,
            CassandraConnector connector, HectorFactoryWrapper hector,
            @Named("LastUpdatePerChannelMap") ConcurrentMap<String, DataHubKey> lastUpdatedPerChannel, LastKeyFinder lastKeyFinder) {
        this.channelsCollection = channelsCollection;
        this.cassandraValueWriter = cassandraValueWriter;
        this.cassandraValueReader = cassandraValueReader;
        this.keyRenderer = keyRenderer;
        this.rowKeyStrategy = rowKeyStrategy;
        this.connector = connector;
        this.hector = hector;
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.lastKeyFinder = lastKeyFinder;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollection.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String name, Long ttlMillis) {
        logger.info("Creating channel name = " + name + ", with ttlMillis = " + ttlMillis);
        return channelsCollection.createChannel(name, ttlMillis);
    }

    @Override
    public void updateChannelMetadata(ChannelConfiguration newConfig) {
        channelsCollection.updateChannel(newConfig);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        logger.debug("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, data);

        ValueInsertionResult result = cassandraValueWriter.write(channelName, value);
        DataHubKey insertedKey = result.getKey();
        setLastUpdateKey(channelName, insertedKey);
        updateFirstKey(channelName, insertedKey);

        return result;
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

    /**
     * todo - gfm - 11/4/13 - not sure how we're going to support this
     */
    @Override
    public Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime) {

        Optional<DataHubKey> firstKey = findFirstUpdateKey(channelName);
        if (!firstKey.isPresent()) {
            return Collections.emptyList();
        }

        DataHubKey maxKey = new DataHubKey(endTime, Short.MAX_VALUE);
        String minColumnKey = keyRenderer.keyToString(firstKey.get());
        String maxColumnKey = keyRenderer.keyToString(maxKey);
        Keyspace keyspace = connector.getKeyspace();

        List<DataHubKey> result = new LinkedList<>();
        List<String> rowKeys = buildRowKeysInRange(channelName, firstKey.get(), maxKey);
        for (String rowKey : rowKeys) {
            QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult =
                    hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())
                            .setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)
                            .setReturnKeysOnly()
                            .setRange(minColumnKey, maxColumnKey, false, Integer.MAX_VALUE)
                            .setKeys(rowKey, rowKey) //TODO: Should we limit the number coming back!?!?  Possible road trip to OOME city?
                            .execute();
            Collection<DataHubKey> keys = buildKeysFromResults(queryResult);
            result.addAll(keys);
        }

        return result;
    }

    private List<String> buildRowKeysInRange(String channelName, DataHubKey firstKey, DataHubKey maxKey) {
        String current = rowKeyStrategy.buildKey(channelName, firstKey);
        String max = rowKeyStrategy.buildKey(channelName, maxKey);
        List<String> result = new ArrayList<>();
        while (current.compareTo(max) <= 0) {
            result.add(current);
            current = rowKeyStrategy.nextKey(channelName, current);
        }
        return result;

    }

    private Collection<DataHubKey> buildKeysFromResults(QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results) {
        Collection<DataHubKey> keys = new ArrayList<>();
        Collection<Row<String, String, DataHubCompositeValue>> dataHubKeyRows = Collections2.filter(results.get().getList(),
                new DataHubRowKeySelector());
        for (Row<String, String, DataHubCompositeValue> row : dataHubKeyRows) {
            keys.addAll(Collections2.transform(row.getColumnSlice().getColumns(), new KeyRenderer()));
        }
        return keys;
    }

    private class DataHubRowKeySelector implements Predicate<Row<String, String, DataHubCompositeValue>> {
        @Override
        public boolean apply(Row<String, String, DataHubCompositeValue> row) {
            return !channelsCollection.isChannelMetadataRowKey(row.getKey());
        }
    }

    private class KeyRenderer implements Function<HColumn<String, DataHubCompositeValue>, DataHubKey> {
        @Override
        public DataHubKey apply(HColumn<String, DataHubCompositeValue> column) {
            return keyRenderer.fromString(column.getName());
        }
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
