package com.flightstats.hub.dao.aws;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaxItemsEnforcerTest {


    @Mock
    private ContentRetriever contentRetriever;
    @Mock
    private ChannelService channelService;
    @Mock
    private DynamoChannelConfigDao channelConfigDao;
    @Mock
    private ChannelConfig channelConfig1;
    @Mock
    private ChannelConfig channelConfig2;
    @Mock
    private ChannelConfig channelConfig3;
    ContentKey latestKey = new ContentKey(TimeUtil.now().plusDays(1));
    SortedSet<ContentKey> keys = Stream.of(
            new ContentKey(latestKey.getTime().minusHours(1)),
            new ContentKey(latestKey.getTime().minusHours(1)),
            new ContentKey(latestKey.getTime().minusHours(1)),
            new ContentKey(latestKey.getTime().minusHours(1)),
            new ContentKey(latestKey.getTime().minusHours(1)),
            latestKey).collect(Collectors.toCollection(TreeSet::new));
    String channelToUpdate = "channelConfig1";
    String bucket = "testBucketName";
    MaxItemsEnforcer maxItemsEnforcer;

    @BeforeEach
    void before() {
        // shared when
        when(channelConfig1.getMaxItems()).thenReturn((long) 6);
        when(channelConfig1.getKeepForever()).thenReturn(false);
        when(channelConfig1.getDisplayName()).thenReturn(channelToUpdate);

        when(contentRetriever.getLatest(channelToUpdate, false)).thenReturn(Optional.of(latestKey));
        when(contentRetriever.query(any(DirectionQuery.class))).thenReturn(keys);

        // given
        maxItemsEnforcer = new MaxItemsEnforcer(contentRetriever, channelService, channelConfigDao);
    }


    @Test
    void updateMaxItems_Configurations() {
        // when
        when(channelConfig2.getMaxItems()).thenReturn((long) 2);
        when(channelConfig2.getKeepForever()).thenReturn(false);
        when(channelConfig2.getDisplayName()).thenReturn("channelConfig2");

        when(channelConfig3.getMaxItems()).thenReturn((long) 0);

        // given
        List<ChannelConfig> channels = Stream.of(channelConfig1, channelConfig3, channelConfig2).collect(Collectors.toList());
        maxItemsEnforcer.updateMaxItems(channels);

        // then
        verify(contentRetriever, times(1)).getLatest(channelToUpdate, false);
        verify(contentRetriever, times(1)).getLatest("channelConfig2", false);
        verify(contentRetriever, never()).getLatest("channelConfig3", false);
        verify(contentRetriever, times(1)).query(any(DirectionQuery.class));
        verify(channelService, times(1)).deleteBefore(channelToUpdate, keys.first());
    }

    @Test
    void testUpdateMaxItems_Channel() {
        // when
        when(channelConfigDao.get(channelToUpdate)).thenReturn(channelConfig1);

        // given
        maxItemsEnforcer.updateMaxItems(channelToUpdate);

        // then
        verify(channelService, times(1)).deleteBefore(channelToUpdate, keys.first());
    }
}
