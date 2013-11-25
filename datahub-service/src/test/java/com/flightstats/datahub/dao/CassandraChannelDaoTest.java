package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CassandraChannelDaoTest {

    @Test
    public void testChannelExists() throws Exception {
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.channelExists("thechan")).thenReturn(true);
        when(collection.channelExists("nope")).thenReturn(false);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null, null);
        assertTrue(testClass.channelExists("thechan"));
        assertFalse(testClass.channelExists("nope"));
    }

    @Test
    public void testCreateChannel() throws Exception {
        ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999), null);
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        when(collection.createChannel("foo", null)).thenReturn(expected);
        DataHubKeyGenerator keyGenerator = mock(DataHubKeyGenerator.class);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, keyGenerator, null);
        ChannelConfiguration result = testClass.createChannel("foo", null);
        assertEquals(expected, result);
        verify(keyGenerator).seedChannel("foo");
    }

    @Test
    public void testUpdateChannel() throws Exception {
        ChannelConfiguration newConfig = new ChannelConfiguration("foo", new Date(9999), 30000L);
        CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
        CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null, null);
        testClass.updateChannelMetadata(newConfig);
        verify(collection).updateChannel(newConfig);
    }

    @Test
    public void testInsert() throws Exception {
        // GIVEN
        DataHubKey key = new DataHubKey((short) 1003);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        long millis = 90210L;
        Optional<String> contentType = Optional.of("text/plain");
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), data, millis);
        ValueInsertionResult expected = new ValueInsertionResult(key, null, null);
        DataHubKey lastUpdateKey = new DataHubKey((short) 1000);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        CqlValueOperations inserter = mock(CqlValueOperations.class);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        LastKeyFinder lastUpdatedKeyFinder = mock(LastKeyFinder.class);
        ChannelConfiguration channelConfig = mock(ChannelConfiguration.class);

        // WHEN
        when(channelsCollection.getChannelConfiguration(channelName)).thenReturn(channelConfig);
        when(channelConfig.getTtlMillis()).thenReturn(millis);
        when(timeProvider.getMillis()).thenReturn(millis);
        when(inserter.write(channelName, value, Optional.of((int)millis/1000))).thenReturn(new ValueInsertionResult(key, null, null));
        when(lastUpdatedKeyFinder.queryForLatestKey(channelName)).thenReturn(lastUpdateKey);
        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, inserter, lastUpdatedMap, lastUpdatedKeyFinder, null,
                timeProvider);

        ValueInsertionResult result = testClass.insert(channelName, contentType, Optional.<String>absent(), data);

        // THEN
        assertEquals(expected, result);
    }

    @Test
    public void testInsert_lastUpdateCacheMiss() throws Exception {
        // GIVEN
        DataHubKey key = new DataHubKey((short) 1003);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Optional<String> contentType = Optional.of("text/plain");
        long millis = 90210L;
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), data, millis);
        ValueInsertionResult expected = new ValueInsertionResult(key, null, null);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        CqlValueOperations inserter = mock(CqlValueOperations.class);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        ChannelConfiguration channelConfig = mock(ChannelConfiguration.class);

        // WHEN
        when(channelsCollection.getChannelConfiguration(channelName)).thenReturn(channelConfig);
        when(channelConfig.getTtlMillis()).thenReturn(millis);
        when(inserter.write(channelName, value, Optional.of((int) millis / 1000))).thenReturn(new ValueInsertionResult(key, null, null));
        when(timeProvider.getMillis()).thenReturn(millis);
        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, inserter, lastUpdatedMap, null, null, timeProvider) {
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
        DataHubKey key = new DataHubKey((short) 1001);
        DataHubKey previousKey = new DataHubKey((short) 1000);
        DataHubKey nextKey = new DataHubKey((short) 1002);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        DataHubCompositeValue compositeValue = new DataHubCompositeValue(Optional.of("text/plain"), null, data, 0L);
        Optional<DataHubKey> previous = Optional.of(previousKey);
        Optional<DataHubKey> next = Optional.of(nextKey);
        LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(compositeValue, previous, next);

        CqlValueOperations inserter = mock(CqlValueOperations.class);

        when(inserter.read(channelName, key)).thenReturn(compositeValue);

        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        when(lastUpdatedMap.get(channelName)).thenReturn(nextKey);
        CassandraChannelDao testClass = new CassandraChannelDao(null, inserter, lastUpdatedMap, null, null, null);

        Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
        assertEquals(expected, result.get());
    }

    @Test
    public void testGetValue_notFound() throws Exception {
        String channelName = "cccccc";
        DataHubKey key = new DataHubKey((short) 1000);

        CqlValueOperations inserter = mock(CqlValueOperations.class);

        when(inserter.read(channelName, key)).thenReturn(null);

        CassandraChannelDao testClass = new CassandraChannelDao(null, inserter, null, null, null, null);

        Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLatestId_cachedInMap() throws Exception {
        DataHubKey expected = new DataHubKey((short) 1006);
        String channelName = "myChan";

        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

        when(lastUpdatedMap.get(channelName)).thenReturn(expected);

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, lastUpdatedMap,
                null, null, null);

        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);
        assertEquals(expected, result.get());
    }

    @Test
    public void testFindLatestId_lazyLoadCacheMiss() throws Exception {
        // GIVEN
        String channelName = "myChan";
        DataHubKey lastUpdateKey = new DataHubKey((short) 1005);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        LastKeyFinder lastKeyFinder = mock(LastKeyFinder.class);

        // WHEN
        when(lastUpdatedMap.get(channelName)).thenReturn(null);
        when(lastKeyFinder.queryForLatestKey(channelName)).thenReturn(lastUpdateKey);

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, lastUpdatedMap, lastKeyFinder, null, null);
        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);

        // THEN
        assertEquals(Optional.of(lastUpdateKey), result);
        verify(lastUpdatedMap).putIfAbsent(channelName, lastUpdateKey);
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

        CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, lastUpdatedMap, lastKeyFinder, null, null);

        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);

        // THEN
        assertEquals(Optional.<DataHubKey>absent(), result);
    }

}
