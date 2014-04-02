package com.flightstats.hub.replication;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SequenceFinderTest {

    public static final String URL = "http://nowhere/channel/blast/";
    public static final String CHANNEL = "blast";
    private static ChannelService channelService;
    private static ChannelUtils channelUtils;
    private ChannelConfiguration configuration;
    private Channel channel;
    private SequenceFinder sequenceFinder;

    @Before
    public void setupClass() throws Exception {
        channelService = mock(ChannelService.class);
        channelUtils = mock(ChannelUtils.class);
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        when(channelService.channelExists(CHANNEL)).thenReturn(false);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(2000)));
        sequenceFinder = new SequenceFinder(channelService, channelUtils);
        channel = new Channel(CHANNEL, URL);
        channel.setConfiguration(configuration);
    }

    @Test
    public void testStartingSequenceMissing() throws Exception {
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.<ContentKey>absent());
        assertEquals(-1, sequenceFinder.getLastUpdated(channel, 0));
    }

    @Test
    public void testStartingSequenceNewTtl() throws Exception {
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        channel.setConfiguration(configuration);
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
        assertEquals(1500, sequenceFinder.getLastUpdated(channel, 20));
    }


    @Test
    public void testStartingSequenceNewHistorical() throws Exception {
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        channel.setConfiguration(configuration);
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
        assertEquals(1500, sequenceFinder.getLastUpdated(channel, 10));
    }

    @Test
    public void testStartingSequenceResumeTtlGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up
        // the sequence is older than the ttl, so we need to pick up with a gap
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        channel.setConfiguration(configuration);
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
        assertEquals(5511, sequenceFinder.getLastUpdated(channel, 20));
    }

    @Test
    public void testStartingSequenceNewReplication() throws Exception {
        //this is the case when a new replication channel is created
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        channel.setConfiguration(configuration);
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
        assertEquals(999, sequenceFinder.getLastUpdated(channel, 20));
    }

    @Test
    public void testStartingSequenceResumeTtlNoGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is not older than the ttl
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        channel.setConfiguration(configuration);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusDays(9)));
        assertEquals(4999, sequenceFinder.getLastUpdated(channel, 20));
    }

    @Test
    public void testStartingSequenceResumeHistoricalGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is older than HistoricalDays
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        channel.setConfiguration(configuration);
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
        assertEquals(5511, sequenceFinder.getLastUpdated(channel, 10));
    }

    @Test
    public void testStartingSequenceResumeHistoricalNoGap() throws Exception {
        //this is the case when replication stopped, and we need to pick back up, the sequence is not older than HistoricalDays
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        channel.setConfiguration(configuration);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(5000)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusDays(9)));
        assertEquals(4999, sequenceFinder.getLastUpdated(channel, 10));
    }

    @Test
    public void testStartingSequenceResumeHistoricalNoGapBuffer() throws Exception {
        //this is the case when replication stopped, and we need to pick back up,
        // the sequence is slighty older than HistoricalDays, but not as old as HistoricalDays + 1
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(20)).build();
        channel.setConfiguration(configuration);
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
        assertEquals(5200, sequenceFinder.getLastUpdated(channel, 10));
    }

    @Test
    public void testStartingSequenceHistorical() throws Exception {
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
        assertEquals(2000, sequenceFinder.getLastUpdated(channel, 2));
    }

    @Test
    public void testStartingSequenceLatest() throws Exception {
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).thenReturn(Optional.of(new DateTime().minusMinutes(1)));
        assertEquals(5999, sequenceFinder.getLastUpdated(channel, 0));
    }
}
