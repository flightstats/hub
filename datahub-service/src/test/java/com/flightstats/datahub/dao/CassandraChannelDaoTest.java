package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CassandraChannelDaoTest {

    @Test
    public void testChannelExists() throws Exception {
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.channelExists("thechan")).thenReturn(true);
        when(collection.channelExists("nope")).thenReturn(false);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null);
        assertTrue(testClass.channelExists("thechan"));
        assertFalse(testClass.channelExists("nope"));
    }

    @Test
    public void testCreateChannel() throws Exception {
        ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999), null);
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.createChannel("foo", null)).thenReturn(expected);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null);
        ChannelConfiguration result = testClass.createChannel("foo", null);
        assertEquals(expected, result);
    }

    @Test
    public void testUpdateChannel() throws Exception {
        ChannelConfiguration newConfig = new ChannelConfiguration("foo", new Date(9999), 30000L);
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null);
        testClass.updateChannelMetadata(newConfig);
        verify(collection).updateChannel(newConfig);
    }

    @Test
    public void testInsert() throws Exception {
        // GIVEN
        DataHubKey key = new DataHubKey((short) 3);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Optional<String> contentType = Optional.of("text/plain");
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), data);
        ValueInsertionResult expected = new ValueInsertionResult(key);
        DataHubKey lastUpdateKey = new DataHubKey((short) 0);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        CassandraValueWriter inserter = mock(CassandraValueWriter.class);
        CassandraValueReader reader = mock(CassandraValueReader.class);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        LastKeyFinder lastUpdatedKeyFinder = mock(LastKeyFinder.class);

        // WHEN
        when(inserter.write(channelName, value, 0)).thenReturn(new ValueInsertionResult(key));
        when(lastUpdatedKeyFinder.queryForLatestKey(channelName)).thenReturn(lastUpdateKey);
        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, inserter, reader, lastUpdatedMap, lastUpdatedKeyFinder
        );

        ValueInsertionResult result = testClass.insert(channelName, contentType, Optional.<String>absent(), data);

        // THEN
        assertEquals(expected, result);
    }

    @Test
    public void testInsert_lastUpdateCacheMiss() throws Exception {
        // GIVEN
        DataHubKey key = new DataHubKey((short) 3);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Optional<String> contentType = Optional.of("text/plain");
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), data);
        ValueInsertionResult expected = new ValueInsertionResult(key);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        CassandraValueWriter inserter = mock(CassandraValueWriter.class);
        CassandraValueReader reader = mock(CassandraValueReader.class);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);

        // WHEN
        when(inserter.write(channelName, value, 0)).thenReturn(new ValueInsertionResult(key));
        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, inserter, reader, lastUpdatedMap, null
        ) {
            @Override
            public Optional<DataHubKey> findLastUpdatedKey(String channelName) {
                return Optional.absent();
            }
        };

        ValueInsertionResult result = testClass.insert(channelName, contentType, Optional.<String>absent(), data);

        // THEN
        assertEquals(expected, result);
    }

    @Test
    public void testGetValue() throws Exception {
        String channelName = "cccccc";
        DataHubKey key = new DataHubKey((short) 1);
        DataHubKey previousKey = new DataHubKey((short) 0);
        DataHubKey nextKey = new DataHubKey((short) 2);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        DataHubCompositeValue compositeValue = new DataHubCompositeValue(Optional.of("text/plain"), null, data);
        Optional<DataHubKey> previous = Optional.of(previousKey);
        Optional<DataHubKey> next = Optional.of(nextKey);
        LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(compositeValue, previous, next);

        CassandraValueReader reader = mock(CassandraValueReader.class);

        when(reader.read(channelName, key)).thenReturn(compositeValue);

        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        when(lastUpdatedMap.get(channelName)).thenReturn(nextKey);
        CassandraChannelDao testClass = new CassandraChannelDao(null, null, reader, lastUpdatedMap, null);

        Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
        assertEquals(expected, result.get());
    }

    @Test
    public void testGetValue_notFound() throws Exception {
        String channelName = "cccccc";
        DataHubKey key = new DataHubKey((short) 0);

        CassandraValueReader reader = mock(CassandraValueReader.class);

        when(reader.read(channelName, key)).thenReturn(null);

        CassandraChannelDao testClass = new CassandraChannelDao(null, null, reader, null, null);

        Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLatestId_cachedInMap() throws Exception {
        DataHubKey expected = new DataHubKey((short) 6);
        String channelName = "myChan";

        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

        when(lastUpdatedMap.get(channelName)).thenReturn(expected);

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, null, lastUpdatedMap,
                null);

        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);
        assertEquals(expected, result.get());
    }

    @Test
    public void testFindLatestId_lazyLoadCacheMiss() throws Exception {
        // GIVEN
        String channelName = "myChan";
        DataHubKey lastUpdateKey = new DataHubKey((short) 5);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        LastKeyFinder lastKeyFinder = mock(LastKeyFinder.class);

        // WHEN
        when(lastUpdatedMap.get(channelName)).thenReturn(null);
        when(lastKeyFinder.queryForLatestKey(channelName)).thenReturn(lastUpdateKey);

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, null, lastUpdatedMap, lastKeyFinder
        );
        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);

        // THEN
        assertEquals(Optional.of(lastUpdateKey), result);
        verify(lastUpdatedMap).put(channelName, lastUpdateKey);
    }


    @Test
    public void testFindLatestId_lastUpdateNotFound() throws Exception {
        // GIVEN
        String channelName = "myChan";
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        LastKeyFinder lastKeyFinder = mock(LastKeyFinder.class);

        // WHEN
        when(lastUpdatedMap.get(channelName)).thenReturn(null);
        when(lastKeyFinder.queryForLatestKey(channelName)).thenReturn(null);

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, null, lastUpdatedMap, lastKeyFinder
        );
        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);

        // THEN
        assertEquals(Optional.<DataHubKey>absent(), result);
    }

    @Test
    public void testFindKeysInRange() throws Exception {
        String channelName = "myChan";
        ChannelConfiguration config = new ChannelConfiguration(channelName, null, null);
        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
        CassandraConnector connector = mock(CassandraConnector.class);
        Keyspace keyspace = mock(Keyspace.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = new YearMonthDayRowKeyStrategy();
        RangeSlicesQuery<String, String, DataHubCompositeValue> query = mock(RangeSlicesQuery.class);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResults = mock(QueryResult.class);
        OrderedRows<String, String, DataHubCompositeValue> queryResultsGuts = mock(OrderedRows.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(query);
        when(query.setColumnFamily(anyString())).thenReturn(query);
        when(query.setKeys(anyString(), anyString())).thenReturn(query);
        when(query.setColumnFamily(anyString())).thenReturn(query);
        when(query.setRange(anyString(), anyString(), anyBoolean(), anyInt())).thenReturn(query);
        when(query.execute()).thenReturn(queryResults);
        when(queryResults.get()).thenReturn(queryResultsGuts);
        when(queryResultsGuts.getList()).thenReturn(Collections.<Row<String, String, DataHubCompositeValue>>emptyList());
        when(channelsCollection.getChannelConfiguration(channelName)).thenReturn(config);
        when(connector.getKeyspace()).thenReturn(keyspace);
        when(keyspace.getKeyspaceName()).thenReturn("datahub");

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, null, null, null);
        Collection<DataHubKey> result = testClass.findKeysInRange(channelName, new Date(0), new Date());
        assertEquals(Collections.emptyList(), result);
    }
}
