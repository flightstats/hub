package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.ClusterStateDao;
import com.flightstats.hub.cluster.LatestContentCache;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ClusterContentServiceTest {
    @Mock
    private ClusterContentService ccs;
    @Mock
    private ContentRetriever contentRetriever;
    @Mock
    private ContentDao mockSpokeWriteDao;
    @Mock
    private ContentDao mockSpokeReadDao;
    @Mock
    private ContentDao mockS3SingleDao;
    @Mock
    private ContentDao mockS3LargeDao;
    @Mock
    private ContentDao mockS3BatchDao;
    @Mock
    private LatestContentCache latestContentCache;
    @Mock
    private S3WriteQueue s3WriteQueue;
    @Mock
    private ChannelConfig channelConfig;
    @Mock
    private ClusterStateDao clusterStateDao;
    @Mock
    private HubUtils hubUtils;
    @Mock
    private AppProperties appProperties;
    @Mock
    private SpokeProperties spokeProperties;
    @Mock
    private ContentProperties contentProperties;

    private Content content;
    private ContentKey contentKey;
    private String channelName;
    private LargeContentUtils largeContentUtils;

    @BeforeEach
    void initClusterContentService() {
        channelName = "/testChannel";
        when(contentRetriever.getCachedChannelConfig(channelName)).thenReturn(Optional.of(channelConfig));
        largeContentUtils = new LargeContentUtils(HubBindings.objectMapper());
        ccs = new ClusterContentService(
                mockSpokeWriteDao, mockSpokeReadDao,
                mockS3SingleDao, mockS3LargeDao, mockS3BatchDao,
                latestContentCache,
                s3WriteQueue, contentRetriever, clusterStateDao, hubUtils,
                largeContentUtils, appProperties, contentProperties, spokeProperties);
    }

    @Test
    @SneakyThrows
    void testSingleNormalInsertWritesContentToSpokeAndEnqueuesS3Write() {
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
    void testSingleNormalInsertDoesntWriteToS3IfBatch() {
        initSimpleContent(false);
        when(channelConfig.isBatch()).thenReturn(true);

        ccs.insert(channelName, content);
        verifyZeroInteractions(s3WriteQueue);
    }

    @Test
    @SneakyThrows
    void testLargePayloadWritesContentToS3AndIndexToSpokeAndS3() {
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