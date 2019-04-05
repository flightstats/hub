package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClusterContentServiceTest {
    @Mock private ClusterContentService ccs;
    @Mock private ChannelService channelSvc;
    @Mock private ContentDao mockSpokeWriteDao;
    @Mock private ContentDao mockSpokeReadDao;
    @Mock private ContentDao mockS3SingleDao;
    @Mock private ContentDao mockS3LargeDao;
    @Mock private ContentDao mockS3BatchDao;
    @Mock private S3WriteQueue s3WriteQueue;
    @Mock private ChannelConfig channelConfig;
    @Mock private LastContentPath lastContentPath;
    @Mock private HubUtils hubUtils;

    private Content content;
    private ContentKey contentKey;
    private String channelName;
    private LargeContentUtils largeContentUtils;

    @Before
    public void initClusterContentService() {
        channelName = "/testChannel";
        when(channelSvc.getCachedChannelConfig(channelName)).thenReturn(Optional.of(channelConfig));
        largeContentUtils = new LargeContentUtils(HubBindings.objectMapper());
        ccs = new ClusterContentService(
                channelSvc,
                mockSpokeWriteDao, mockSpokeReadDao,
                mockS3SingleDao, mockS3LargeDao, mockS3BatchDao,
                s3WriteQueue, lastContentPath, hubUtils,
                largeContentUtils);
    }

    @Test
    @SneakyThrows
    public void testSingleNormalInsertWritesContentToSpokeAndEnqueuesS3Write() {
        initSimpleContent(false);
        when(channelConfig.isBatch()).thenReturn(false);

        ContentKey ret = ccs.insert(channelName, content);
        verify(mockSpokeWriteDao, times(1)).insert(channelName, content);
        assertNotNull(ret);
        assertEquals(contentKey, ret);

        ArgumentCaptor<ChannelContentKey> arg = ArgumentCaptor.forClass(ChannelContentKey.class);
        verify(s3WriteQueue, times(1)).add(arg.capture());
        assertNotNull(arg.getValue());
        assertEquals(contentKey, arg.getValue().getContentKey());
        assertEquals(channelName, arg.getValue().getChannel());

        verifyZeroInteractions(mockS3LargeDao);
    }

    @Test
    @SneakyThrows
    public void testSingleNormalInsertDoesntWriteToS3IfBatch() {
        initSimpleContent(false);
        when(channelConfig.isBatch()).thenReturn(true);

        ccs.insert(channelName, content);
        verifyZeroInteractions(s3WriteQueue);
    }

    @Test
    @SneakyThrows
    public void testLargePayloadWritesContentToS3AndIndexToSpokeAndS3() {
        initSimpleContent(true);
        when(channelConfig.isBatch()).thenReturn(false);

        ccs.insert(channelName, content);
        verify(mockS3LargeDao, times(1)).insert(channelName, content);
        verify(mockSpokeWriteDao, times(1)).insert(channelName, largeContentUtils.createIndex(content));
        verify(s3WriteQueue, times(1)).add(any());
    }

    @SneakyThrows
    private void initSimpleContent(boolean isLarge) {
        byte[] data = "SimpleTest".getBytes();
        contentKey = new ContentKey(TimeUtil.now(), "someHash");
        content = Content.builder()
                .withContentType("text/plain")
                .withContentKey(contentKey)
                .withLarge(isLarge)
                .withData(data).build();
        when(mockSpokeWriteDao.insert(anyString(), any(Content.class))).thenReturn(contentKey);
    }

}