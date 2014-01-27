package com.flightstats.datahub.replication;

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

    @Before
    public void setupClass() throws Exception {
        channelService = mock(ChannelService.class);
        channelUtils = mock(ChannelUtils.class);
        configuration = ChannelConfiguration.builder().withName(CHANNEL).withTtlMillis(TimeUnit.DAYS.toMillis(10)).build();
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.of(configuration));
        when(channelService.channelExists(CHANNEL)).thenReturn(false);
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(2000)));
        replicator = new ChannelReplicator(channelService, URL, channelUtils, null);
    }

    @Test
    public void testLifeCycleNew() throws Exception {
        Optional<Content> optional = Optional.of(mock(Content.class));
        when(channelUtils.getContent(URL, 2001)).thenReturn(optional);
        when(channelUtils.getContent(URL, 2002)).thenReturn(optional);
        when(channelUtils.getContent(URL, 2003)).thenReturn(Optional.<Content>absent());
        replicator.doWork();
        verify(channelService).createChannel(configuration);
        verify(channelService, new Times(2)).insert(CHANNEL, optional.get());
    }

    @Test
    public void testCreateChannelAbsent() throws Exception {
        when(channelUtils.getConfiguration(URL)).thenReturn(Optional.<ChannelConfiguration>absent());
        replicator.doWork();
        verify(channelService, never()).createChannel(any(ChannelConfiguration.class));
    }

    @Test
    public void testStartingSequenceMissing() throws Exception {
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.<ContentKey>absent());
        assertEquals(-1, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceExisting() throws Exception {
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(3000)));
        assertEquals(3001, replicator.getStartingSequence());
    }

    @Test
    public void testStartingSequenceNew() throws Exception {
        replicator.initialize();
        when(channelService.findLastUpdatedKey(CHANNEL)).thenReturn(Optional.of((ContentKey) new SequenceContentKey(SequenceContentKey.START_VALUE)));
        when(channelUtils.getLatestSequence(URL)).thenReturn(Optional.of(6000L));
        when(channelUtils.getCreationDate(anyString(), anyLong())).then(new Answer<Optional<DateTime>>() {
            @Override
            public Optional<DateTime> answer(InvocationOnMock invocation) throws Throwable {
                long aLong = (long) invocation.getArguments()[1];
                if (aLong > 1500) {
                    return Optional.of(new DateTime());
                }
                return Optional.absent();
            }
        });
        assertEquals(1501, replicator.getStartingSequence());
    }

}
