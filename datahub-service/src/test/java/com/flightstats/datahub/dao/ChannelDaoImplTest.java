package com.flightstats.datahub.dao;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.datahub.dao.cassandra.CassandraChannelsCollectionDao;
import com.flightstats.datahub.dao.cassandra.CassandraDataHubValueDao;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChannelDaoImplTest {

    private MetricsTimer metricsTimer;

    @Before
    public void setUp() throws Exception {
        metricsTimer = new MetricsTimer(new MetricRegistry());
    }

    @Test
    public void testChannelExists() throws Exception {
        ChannelsCollectionDao collection = mock(CassandraChannelsCollectionDao.class);
        when(collection.channelExists("thechan")).thenReturn(true);
        when(collection.channelExists("nope")).thenReturn(false);
        ChannelDaoImpl testClass = new ChannelDaoImpl(collection, null, null, null, null, metricsTimer);
        assertTrue(testClass.channelExists("thechan"));
        assertFalse(testClass.channelExists("nope"));
    }

    @Test
    public void testCreateChannel() throws Exception {
        ChannelConfiguration expected = ChannelConfiguration.builder()
                .withCreationDate(new Date(9999))
                .withName("foo").build();
        ChannelsCollectionDao collection = mock(CassandraChannelsCollectionDao.class);
        when(collection.createChannel("foo", null)).thenReturn(expected);
        DataHubKeyGenerator keyGenerator = mock(DataHubKeyGenerator.class);
        DataHubValueDao valueDao = mock(DataHubValueDao.class);
        ChannelDaoImpl testClass = new ChannelDaoImpl(collection, valueDao, null, null, null, metricsTimer);
        ChannelConfiguration result = testClass.createChannel("foo", null);
        assertEquals(expected, result);
    }

    @Test
    public void testUpdateChannel() throws Exception {
        ChannelConfiguration newConfig = ChannelConfiguration.builder()
                .withCreationDate(new Date(9999))
                .withName("foo").build();
        ChannelsCollectionDao collection = mock(CassandraChannelsCollectionDao.class);
        ChannelDaoImpl testClass = new ChannelDaoImpl(collection, null, null, null, null, metricsTimer);
        testClass.updateChannelMetadata(newConfig);
        verify(collection).updateChannel(newConfig);
    }

    @Test
    public void testInsert() throws Exception {
        // GIVEN
        DataHubKey key = new SequenceDataHubKey( 1003);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        long millis = 90210L;
        Optional<String> contentType = Optional.of("text/plain");
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), data, millis);
        ValueInsertionResult expected = new ValueInsertionResult(key, null, null);
        DataHubKey lastUpdateKey = new SequenceDataHubKey( 1000);

        ChannelsCollectionDao channelsCollectionDao = mock(CassandraChannelsCollectionDao.class);
        DataHubValueDao inserter = mock(CassandraDataHubValueDao.class);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        ChannelConfiguration channelConfig = mock(ChannelConfiguration.class);
        ChannelInsertionPublisher publisher = mock(ChannelInsertionPublisher.class);

        // WHEN
        when(channelsCollectionDao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
        when(channelConfig.getTtlMillis()).thenReturn(millis);
        when(timeProvider.getMillis()).thenReturn(millis);
        when(inserter.write(channelName, value, Optional.of((int)millis/1000))).thenReturn(new ValueInsertionResult(key, null, null));
        ChannelDaoImpl testClass = new ChannelDaoImpl(channelsCollectionDao, inserter, lastUpdatedMap,
                timeProvider, publisher, metricsTimer);

        ValueInsertionResult result = testClass.insert(channelName, contentType, Optional.<String>absent(), data);

        // THEN
        assertEquals(expected, result);
    }

    @Test
    public void testInsert_lastUpdateCacheMiss() throws Exception {
        // GIVEN
        DataHubKey key = new SequenceDataHubKey(1003);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Optional<String> contentType = Optional.of("text/plain");
        long millis = 90210L;
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), data, millis);
        ValueInsertionResult expected = new ValueInsertionResult(key, null, null);

        ChannelsCollectionDao channelsCollectionDao = mock(CassandraChannelsCollectionDao.class);
        DataHubValueDao inserter = mock(CassandraDataHubValueDao.class);
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        ChannelConfiguration channelConfig = mock(ChannelConfiguration.class);
        ChannelInsertionPublisher publisher = mock(ChannelInsertionPublisher.class);

        // WHEN
        when(channelsCollectionDao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
        when(channelConfig.getTtlMillis()).thenReturn(millis);
        when(inserter.write(channelName, value, Optional.of((int) millis / 1000))).thenReturn(new ValueInsertionResult(key, null, null));
        when(timeProvider.getMillis()).thenReturn(millis);
        ChannelDaoImpl testClass = new ChannelDaoImpl(channelsCollectionDao, inserter, lastUpdatedMap, timeProvider, publisher, metricsTimer) {
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
        DataHubKey key = new SequenceDataHubKey( 1001);
        DataHubKey previousKey = new SequenceDataHubKey( 1000);
        DataHubKey nextKey = new SequenceDataHubKey( 1002);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        DataHubCompositeValue compositeValue = new DataHubCompositeValue(Optional.of("text/plain"), null, data, 0L);
        Optional<DataHubKey> previous = Optional.of(previousKey);
        Optional<DataHubKey> next = Optional.of(nextKey);
        LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(compositeValue, previous, next);

        DataHubValueDao inserter = mock(CassandraDataHubValueDao.class);

        when(inserter.read(channelName, key)).thenReturn(compositeValue);

        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        when(lastUpdatedMap.get(channelName)).thenReturn(nextKey);
        ChannelDaoImpl testClass = new ChannelDaoImpl(null, inserter, lastUpdatedMap, null, null, metricsTimer);

        Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
        assertEquals(expected, result.get());
    }

    @Test
    public void testGetValue_notFound() throws Exception {
        String channelName = "cccccc";
        DataHubKey key = new SequenceDataHubKey( 1000);

        DataHubValueDao inserter = mock(CassandraDataHubValueDao.class);

        when(inserter.read(channelName, key)).thenReturn(null);

        ChannelDaoImpl testClass = new ChannelDaoImpl(null, inserter, null, null, null, metricsTimer);

        Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLatestId_cachedInMap() throws Exception {
        DataHubKey expected = new SequenceDataHubKey( 1006);
        String channelName = "myChan";

        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        ChannelsCollectionDao channelsCollectionDao = mock(CassandraChannelsCollectionDao.class);

        when(lastUpdatedMap.get(channelName)).thenReturn(expected);

        ChannelDaoImpl testClass = new ChannelDaoImpl(channelsCollectionDao, null, lastUpdatedMap,
                null, null, metricsTimer);

        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);
        assertEquals(expected, result.get());
    }

    @Test
    public void testFindLatestId_lastUpdateNotFound() throws Exception {
        // GIVEN
        String channelName = "myChan";
        ConcurrentMap<String, DataHubKey> lastUpdatedMap = mock(ConcurrentMap.class);
        ChannelsCollectionDao channelsCollectionDao = mock(CassandraChannelsCollectionDao.class);

        // WHEN
        when(lastUpdatedMap.get(channelName)).thenReturn(null);

        ChannelDaoImpl testClass = new ChannelDaoImpl(channelsCollectionDao, null, lastUpdatedMap, null, null, metricsTimer);

        Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);

        // THEN
        assertEquals(Optional.<SequenceDataHubKey>absent(), result);
    }

}
