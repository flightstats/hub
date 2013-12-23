package com.flightstats.datahub.dao;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.datahub.dao.cassandra.CassandraChannelMetadataDao;
import com.flightstats.datahub.dao.cassandra.CassandraContentDao;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelDaoImplTest {

    private MetricsTimer metricsTimer;

    @Before
    public void setUp() throws Exception {
        metricsTimer = new MetricsTimer(new MetricRegistry());
    }

    @Test
    public void testInsert() throws Exception {
        // GIVEN
        ContentKey key = new SequenceContentKey( 1003);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        long millis = 90210L;
        Optional<String> contentType = Optional.of("text/plain");
        Content value = new Content(contentType, Optional.<String>absent(), data, millis);
        ValueInsertionResult expected = new ValueInsertionResult(key, null);

        ChannelMetadataDao channelMetadataDao = mock(CassandraChannelMetadataDao.class);
        ContentDao inserter = mock(CassandraContentDao.class);
        ConcurrentMap<String, ContentKey> lastUpdatedMap = mock(ConcurrentMap.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        ChannelConfiguration channelConfig = ChannelConfiguration.builder().withName(channelName).withTtlMillis(millis).build();
        ChannelInsertionPublisher publisher = mock(ChannelInsertionPublisher.class);

        // WHEN
        when(channelMetadataDao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
        when(timeProvider.getMillis()).thenReturn(millis);
        when(inserter.write(channelName, value, Optional.of((int)millis/1000))).thenReturn(new ValueInsertionResult(key, null));
        ContentServiceImpl testClass = new ContentServiceImpl(inserter, lastUpdatedMap,
                timeProvider, publisher, metricsTimer);


        ValueInsertionResult result = testClass.insert(channelConfig, contentType, Optional.<String>absent(), data);

        // THEN
        assertEquals(expected, result);
    }

    @Test
    public void testInsert_lastUpdateCacheMiss() throws Exception {
        // GIVEN
        ContentKey key = new SequenceContentKey(1003);
        String channelName = "foo";
        byte[] data = "bar".getBytes();
        Optional<String> contentType = Optional.of("text/plain");
        long millis = 90210L;
        Content value = new Content(contentType, Optional.<String>absent(), data, millis);
        ValueInsertionResult expected = new ValueInsertionResult(key, null);

        ChannelMetadataDao channelMetadataDao = mock(CassandraChannelMetadataDao.class);
        ContentDao inserter = mock(CassandraContentDao.class);
        ConcurrentMap<String, ContentKey> lastUpdatedMap = mock(ConcurrentMap.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        ChannelConfiguration channelConfig = ChannelConfiguration.builder().withName(channelName).withTtlMillis(millis).build();
        ChannelInsertionPublisher publisher = mock(ChannelInsertionPublisher.class);

        // WHEN
        when(channelMetadataDao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
        when(inserter.write(channelName, value, Optional.of((int) millis / 1000))).thenReturn(new ValueInsertionResult(key, null));
        when(timeProvider.getMillis()).thenReturn(millis);
        ContentServiceImpl testClass = new ContentServiceImpl(inserter, lastUpdatedMap, timeProvider, publisher, metricsTimer) {
            @Override
            public Optional<ContentKey> findLastUpdatedKey(String channelName) {
                return Optional.absent();
            }
        };

        ValueInsertionResult result = testClass.insert(channelConfig, contentType, Optional.<String>absent(), data);

        // THEN
        assertEquals(expected, result);
    }

    @Test
    public void testGetValue() throws Exception {
        String channelName = "cccccc";
        ContentKey key = new SequenceContentKey( 1001);
        ContentKey previousKey = new SequenceContentKey( 1000);
        ContentKey nextKey = new SequenceContentKey( 1002);
        byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
        Content compositeValue = new Content(Optional.of("text/plain"), null, data, 0L);
        Optional<ContentKey> previous = Optional.of(previousKey);
        Optional<ContentKey> next = Optional.of(nextKey);
        LinkedContent expected = new LinkedContent(compositeValue, previous, next);

        ContentDao valueDao = mock(ContentDao.class);

        when(valueDao.read(channelName, key)).thenReturn(compositeValue);
        when(valueDao.getKey(key.keyToString())).thenReturn(Optional.of(key));

        ConcurrentMap<String, ContentKey> lastUpdatedMap = mock(ConcurrentMap.class);
        when(lastUpdatedMap.get(channelName)).thenReturn(nextKey);
        ContentServiceImpl testClass = new ContentServiceImpl(valueDao, lastUpdatedMap, null, null, metricsTimer);

        Optional<LinkedContent> result = testClass.getValue(channelName, key.keyToString());
        assertEquals(expected, result.get());
    }

    @Test
    public void testGetValue_notFound() throws Exception {
        String channelName = "cccccc";
        ContentKey key = new SequenceContentKey( 1000);

        ContentDao valueDao = mock(ContentDao.class);

        when(valueDao.read(channelName, key)).thenReturn(null);
        when(valueDao.getKey(key.keyToString())).thenReturn(Optional.of(key));

        ContentServiceImpl testClass = new ContentServiceImpl(valueDao, null, null, null, metricsTimer);

        Optional<LinkedContent> result = testClass.getValue(channelName, key.keyToString());
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLatestId_cachedInMap() throws Exception {
        ContentKey expected = new SequenceContentKey( 1006);
        String channelName = "myChan";

        ConcurrentMap<String, ContentKey> lastUpdatedMap = mock(ConcurrentMap.class);

        when(lastUpdatedMap.get(channelName)).thenReturn(expected);

        ContentServiceImpl testClass = new ContentServiceImpl(null, lastUpdatedMap,
                null, null, metricsTimer);

        Optional<ContentKey> result = testClass.findLastUpdatedKey(channelName);
        assertEquals(expected, result.get());
    }

    @Test
    public void testFindLatestId_lastUpdateNotFound() throws Exception {
        // GIVEN
        String channelName = "myChan";
        ConcurrentMap<String, ContentKey> lastUpdatedMap = mock(ConcurrentMap.class);

        // WHEN
        when(lastUpdatedMap.get(channelName)).thenReturn(null);

        ContentServiceImpl testClass = new ContentServiceImpl(null, lastUpdatedMap, null, null, metricsTimer);

        Optional<ContentKey> result = testClass.findLastUpdatedKey(channelName);

        // THEN
        assertEquals(Optional.<SequenceContentKey>absent(), result);
    }

}
