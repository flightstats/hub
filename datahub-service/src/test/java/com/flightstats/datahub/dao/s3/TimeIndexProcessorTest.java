package com.flightstats.datahub.dao.s3;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.cluster.ZooKeeperState;
import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.util.TimeProvider;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TimeIndexProcessorTest {

    private static TestingServer testingServer;
    private static CuratorFramework curator;
    private static S3ContentDao s3ContentDao;
    private Map<String,Set<String>> expected;
    private MockTimeIndexDao indexDao;
    private TimeIndexProcessor processor;
    private DateTime startTime;
    private int key;
    private String channel;
    private TimeProvider timeProvider;
    private DateTime dateTime;

    @BeforeClass
    public static void setupClass() throws Exception {
        testingServer = new TestingServer(2181);
        RetryPolicy retryPolicy = GuiceContextListenerFactory.DatahubCommonModule.buildRetryPolicy();
        curator = GuiceContextListenerFactory.DatahubCommonModule.buildCurator("localhost:2181", retryPolicy, new ZooKeeperState());
        s3ContentDao = new S3ContentDao(null, null, "", curator, new MetricsTimer(new MetricRegistry()));
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        testingServer.stop();
    }

    @Before
    public void setUp() throws Exception {
        expected = new HashMap<>();
        indexDao = new MockTimeIndexDao();
        channel = UUID.randomUUID().toString();
        key = 100;
        timeProvider = mock(TimeProvider.class);
        dateTime = new DateTime();
        when(timeProvider.getDateTime()).thenReturn(dateTime);
        processor = new TimeIndexProcessor(curator, indexDao, timeProvider, new ZooKeeperState());
        startTime = new DateTime(2014, 1, 6, 3, 42, 1);
    }

    @Test
    public void testProcess() throws Exception {
        int numMinutes = 10;
        int totalExpected = setupData(numMinutes);

        processor.process(channel);

        assertEquals(numMinutes, indexDao.indices.size());
        int totalFound = validateData(numMinutes);
        assertEquals(totalExpected, totalFound);
        indexDao.indices.clear();
        processor.process(channel);
        assertEquals(0, indexDao.indices.size());
    }

    @Test
    public void testProcessCurrentTime() throws Exception {
        int numMinutes = 10;
        startTime = dateTime.minusMinutes(5);
        setupData(numMinutes);

        processor.process(channel);

        assertEquals(4, indexDao.indices.size());
        indexDao.indices.clear();
        when(timeProvider.getDateTime()).thenReturn(dateTime.plusMinutes(2));
        processor.process(channel);
        assertEquals(2, indexDao.indices.size());

    }

    private int validateData(int numMinutes) {
        int totalFound = 0;
        for (int minutes = 0; minutes < numMinutes; minutes++) {
            String mapKey = channel + TimeIndex.getHash(startTime.plusMinutes(minutes));
            List<String> keys = indexDao.indices.get(mapKey);
            Set<String> strings = expected.get(mapKey);
            assertEquals(strings.size(), keys.size());
            totalFound += keys.size();
            assertTrue(keys.containsAll(strings));
        }
        return totalFound;
    }

    private int setupData(int numMinutes) {
        int total = 0;
        for (int minutes = 0; minutes < numMinutes; minutes++) {
            DateTime dateTime = startTime.plusMinutes(minutes);
            HashSet<String> set = new HashSet<>();
            expected.put(channel + TimeIndex.getHash(dateTime), set);
            for (int keys = 0; keys < minutes + 1; keys++) {
                total++;
                SequenceContentKey contentKey = new SequenceContentKey(key++);
                s3ContentDao.writeIndex(channel, dateTime, contentKey);
                set.add(contentKey.keyToString());
            }
        }
        return total;
    }

    private class MockTimeIndexDao implements TimeIndexDao {
        Map<String, List<String>> indices = new HashMap<>();

        @Override
        public void writeIndex(String channelName, DateTime dateTime, ContentKey key) {
            //not needed
        }

        @Override
        public void writeIndices(String channelName, String dateTime, List<String> keys) {
            indices.put(channelName + dateTime, keys);
        }
    }
}
