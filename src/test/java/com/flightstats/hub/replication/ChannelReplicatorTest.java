package com.flightstats.hub.replication;

import com.flightstats.hub.app.config.GuiceContext;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import com.google.common.base.Optional;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.verification.AtLeast;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ChannelReplicatorTest {

    public static final String URL = "http://nowhere/channel/blast/";
    public static final String CHANNEL = "blast";
    private static ChannelService channelService;
    private static ChannelUtils channelUtils;
    private ChannelReplicator replicator;
    private ChannelConfiguration configuration;
    private SequenceIterator sequenceIterator;
    private SequenceIteratorFactory factory;
    private Channel channel;
    private SequenceFinder sequenceFinder;
    private static CuratorFramework curator;

    @BeforeClass
    public static void setupClass() throws Exception {
        Integration.startZooKeeper();
        RetryPolicy retryPolicy = GuiceContext.HubCommonModule.buildRetryPolicy();
        curator = GuiceContext.HubCommonModule.buildCurator("hub", "test", "localhost:2181", retryPolicy, new ZooKeeperState());
    }

    @Before
    public void setup() throws Exception {
        channelService = mock(ChannelService.class);
        channelUtils = mock(ChannelUtils.class);
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        when(channelService.channelExists(CHANNEL)).thenReturn(false);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(2000)));
        factory = mock(SequenceIteratorFactory.class);
        sequenceIterator = mock(SequenceIterator.class);
        when(factory.create(anyLong(), any(Channel.class))).thenReturn(sequenceIterator);
        sequenceFinder = new SequenceFinder(channelUtils);
        replicator = new ChannelReplicator(channelService, channelUtils, factory, sequenceFinder, curator);
        channel = new Channel(CHANNEL, URL);
        replicator.setChannel(channel);
    }

    @After
    public void tearDown() throws Exception {
        replicator.exit();
    }

    @Test
    public void testLifeCycleNew() throws Exception {
        Content content = mock(Content.class);
        Optional<Content> optional = Optional.of(content);
        when(sequenceIterator.hasNext()).thenReturn(true);
        when(sequenceIterator.next()).thenReturn(optional);
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.<Long>absent());
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        when(channelService.insert(CHANNEL, content)).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                countDownLatch.countDown();
                return null;
            }
        });
        replicator.verifyRemoteChannel();
        replicator.tryLeadership();

        assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
        verify(channelService, new AtLeast(1)).createChannel(configuration);
        verify(channelService, new AtLeast(2)).insert(CHANNEL, content);
    }

    @Test
    public void testCreateChannelAbsent() throws Exception {
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.<ChannelConfiguration>absent());
        assertFalse(replicator.tryLeadership());
        assertFalse(replicator.isValid());
        verify(channelService, never()).createChannel(any(ChannelConfiguration.class));
    }

    @Test
    public void testStartingSequenceMissing() throws Exception {
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.<ContentKey>absent());
        assertEquals(-1, replicator.getLastUpdated());
    }

    @Test
    public void testTimeSeries() throws Exception {
        ChannelConfiguration timeSeries = ChannelConfiguration.builder().withName("TS").withType(ChannelConfiguration.ChannelType.TimeSeries).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(timeSeries));
        assertFalse(replicator.tryLeadership());

        assertFalse(replicator.isValid());
        verify(channelService, never()).createChannel(any(ChannelConfiguration.class));
    }

    @Test
    public void testStartingSequenceNewTtl() throws Exception {
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(20);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong > 1500) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(11));
            }
        });
        assertEquals(1500, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceNewHistorical() throws Exception {
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(10);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong > 1500) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(11));
            }
        });
        assertEquals(1500, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceResumeTtlGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up
        // the sequence is older than the ttl, so we need to pick up with a gap
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(20);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong > 5511) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(11));
            }
        });
        assertEquals(5511, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceNewReplication() throws Exception {
        //this is the case when a new replication channel is created
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(20);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(999)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong >= 1000) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.absent();
            }
        });
        assertEquals(999, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceResumeTtlNoGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is not older than the ttl
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(20);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusDays(9)));
        assertEquals(4999, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceResumeHistoricalGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is older than HistoricalDays
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(10);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong > 5511) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(12));
            }
        });
        assertEquals(5511, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceResumeHistoricalNoGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is not older than HistoricalDays
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(10);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusDays(9)));
        assertEquals(4999, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceResumeHistoricalNoGapBuffer() throws Exception {
        //this is the case when replication stopped, and we need to pick back up,
        // the sequence is slighty older than HistoricalDays, but not as old as HistoricalDays + 1
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        init(10);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong > 5511) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                if (aLong > 5200) {
                    return Optional.of(new DateTime().minusDays(10).minusHours(23));
                }
                return Optional.of(new DateTime().minusDays(12));
            }
        });
        assertEquals(5200, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceHistorical() throws Exception {
        init(2);

        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (Long) invocation.getArguments()[1];
                if (aLong > 2000) {
                    return Optional.of(new DateTime().minusDays(1));
                }
                return Optional.of(new DateTime().minusDays(3));
            }
        });
        assertEquals(2000, replicator.getLastUpdated());
    }

    @Test
    public void testStartingSequenceLatest() throws Exception {
        init(0);

        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusMinutes(1)));
        assertEquals(5999, replicator.getLastUpdated());
    }

    private void init(int historicalDays) throws IOException {
        replicator.setHistoricalDays(historicalDays);
        assertTrue(replicator.verifyRemoteChannel());
        replicator.initialize();
    }

    @Test
    public void testLostLock() throws Exception {
        CuratorLock curatorLock = mock(CuratorLock.class);
        when(curatorLock.shouldKeepWorking()).thenReturn(false);
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.<Long>absent());
        replicator = new ChannelReplicator(channelService, channelUtils, factory, sequenceFinder, curator);
        replicator.setChannel(channel);
        replicator.verifyRemoteChannel();
        replicator.tryLeadership();
        Sleeper.sleep(20);
        verify(channelService, never()).insert(anyString(), any(Content.class));
    }
}
