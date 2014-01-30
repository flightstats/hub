package com.flightstats.datahub.replication;

import com.flightstats.datahub.cluster.CuratorLock;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * As much as I dislike Mockito, this is going to be difficult to verify, so it is worth the pain of Mockito.
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

    @Before
    public void setupClass() throws Exception {
        channelService = mock(ChannelService.class);
        channelUtils = mock(ChannelUtils.class);
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        when(channelService.channelExists(CHANNEL)).thenReturn(false);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(2000)));
        factory = mock(SequenceIteratorFactory.class);
        sequenceIterator = mock(SequenceIterator.class);
        when(factory.create(anyLong(), anyString())).thenReturn(sequenceIterator);
        CuratorLock curatorLock = mock(CuratorLock.class);
        when(curatorLock.shouldKeepWorking()).thenReturn(true);
        replicator = new ChannelReplicator(channelService, channelUtils, curatorLock, factory);
        channel = new Channel(CHANNEL, URL);
        replicator.setChannel(channel);
    }

    @Test
    public void testLifeCycleNew() throws Exception {
        Content content = mock(Content.class);
        when(sequenceIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(sequenceIterator.next()).thenReturn(content).thenReturn(content).thenReturn(null);
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.<Long>absent());
        replicator.runWithLock();
        verify(channelService).createChannel(configuration);
        verify(channelService, new Times(2)).insert(CHANNEL, content);
    }

    @Test
    public void testCreateChannelAbsent() throws Exception {
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.<ChannelConfiguration>absent());
        replicator.runWithLock();
        verify(channelService, never()).createChannel(any(ChannelConfiguration.class));
    }

    @Test
    public void testStartingSequenceMissing() throws Exception {
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.<ContentKey>absent());
        assertEquals(-1, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceNewTtl() throws Exception {
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(20);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 1500) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(11));
            }
        });
        assertEquals(1501, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceNewHistorical() throws Exception {
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(10);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 1500) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(11));
            }
        });
        assertEquals(1501, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceResumeTtlGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up
        // the sequence is older than the ttl, so we need to pick up with a gap
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(20);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 5511) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(11));
            }
        });
        assertEquals(5512, replicator.getStartingSequence());

    }

    @Test
    public void testStartingSequenceResumeTtlNoGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is not older than the ttl
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(20);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusDays(9)));
        assertEquals(5001, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceResumeHistoricalGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is older than HistoricalDays
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(10);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 5511) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                return Optional.of(new DateTime().minusDays(12));
            }
        });
        assertEquals(5512, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceResumeHistoricalNoGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is not older than HistoricalDays
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(10);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusDays(9)));
        assertEquals(5001, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceResumeHistoricalNoGapBuffer() throws Exception {
        //this is the case when replication stopped, and we need to pick back up,
        // the sequence is slighty older than HistoricalDays, but not as old as HistoricalDays + 1
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        replicator.setHistoricalDays(10);
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 5511) {
                    return Optional.of(new DateTime().minusDays(9));
                }
                if (aLong > 5200) {
                    return Optional.of(new DateTime().minusDays(10).minusHours(23));
                }
                return Optional.of(new DateTime().minusDays(12));
            }
        });
        assertEquals(5201, replicator.getStartingSequence());
    }


    @Test
    public void testStartingSequenceHistorical() throws Exception {
        replicator.setHistoricalDays(2);
        replicator.initialize();

        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 2000) {
                    return Optional.of(new DateTime().minusDays(1));
                }
                return Optional.of(new DateTime().minusDays(3));
            }
        });
        assertEquals(2001, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceLatest() throws Exception {
        replicator.setHistoricalDays(0);
        replicator.initialize();

        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusMinutes(1)));
        assertEquals(6000, replicator.getStartingSequence());
    }

    @Test
    public void testLostLock() throws Exception {
        CuratorLock curatorLock = mock(CuratorLock.class);
        when(curatorLock.shouldKeepWorking()).thenReturn(false);
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.<Long>absent());
        replicator = new ChannelReplicator(channelService, channelUtils, curatorLock, factory);
        replicator.setChannel(channel);
        replicator.runWithLock();
        verify(channelService, never()).insert(anyString(), any(Content.class));
    }

}
