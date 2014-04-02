package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
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
        when(factory.create(anyLong(), any(Channel.class))).thenReturn(sequenceIterator);
        CuratorLock curatorLock = mock(CuratorLock.class);
        when(curatorLock.shouldKeepWorking()).thenReturn(true);
        sequenceFinder = new SequenceFinder(channelService, channelUtils);
        replicator = new ChannelReplicator(channelService, channelUtils, curatorLock, factory, sequenceFinder);
        channel = new Channel(CHANNEL, URL);
        replicator.setChannel(channel);
    }

    @Test
    public void testLifeCycleNew() throws Exception {
        Content content = mock(Content.class);
        Optional<Content> optional = Optional.of(content);
        when(sequenceIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(sequenceIterator.next()).thenReturn(optional).thenReturn(optional).thenReturn(null);
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.<Long>absent());
        replicator.verifyRemoteChannel();
        replicator.runWithLock();
        verify(channelService).createChannel(configuration);
        verify(channelService, new Times(2)).insert(CHANNEL, content);
    }

    @Test
    public void testCreateChannelAbsent() throws Exception {
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.<ChannelConfiguration>absent());
        replicator.run();
        assertFalse(replicator.isValid());
        verify(channelService, never()).createChannel(any(ChannelConfiguration.class));
    }

    @Test
    public void testTimeSeries() throws Exception {
        ChannelConfiguration timeSeries = ChannelConfiguration.builder().withName("TS").withType(ChannelConfiguration.ChannelType.TimeSeries).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(timeSeries));
        replicator.run();
        assertFalse(replicator.isValid());
        verify(channelService, never()).createChannel(any(ChannelConfiguration.class));
    }

    @Test
    public void testLostLock() throws Exception {
        CuratorLock curatorLock = mock(CuratorLock.class);
        when(curatorLock.shouldKeepWorking()).thenReturn(false);
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.<Long>absent());
        replicator = new ChannelReplicator(channelService, channelUtils, curatorLock, factory, sequenceFinder);
        replicator.setChannel(channel);
        replicator.verifyRemoteChannel();
        replicator.runWithLock();
        verify(channelService, never()).insert(anyString(), any(Content.class));
    }

}
