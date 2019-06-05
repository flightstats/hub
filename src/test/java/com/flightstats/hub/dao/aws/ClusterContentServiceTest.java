package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.LatestContentCache;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.ContentProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private ClusterCacheDao clusterCacheDao;
    @Mock
    private HubUtils hubUtils;
    @Mock
    private AppProperties appProperties;
    @Mock
    private SpokeProperties spokeProperties;
    @Mock
    private ContentProperties contentProperties;

    private LargeContentUtils largeContentUtils;
    private String channelName = "/testChannel";
    private Content content;
    private ContentKey contentKey;

    @BeforeEach
    void initClusterContentService() {

        largeContentUtils = new LargeContentUtils(new ObjectMapper());
        ccs = new ClusterContentService(
                mockSpokeWriteDao,
                mockSpokeReadDao,
                mockS3SingleDao,
                mockS3LargeDao,
                mockS3BatchDao,
                latestContentCache,
                s3WriteQueue,
                contentRetriever,
                clusterCacheDao,
                hubUtils,
                largeContentUtils,
                appProperties,
                contentProperties,
                spokeProperties);
    }

    @Test
    @SneakyThrows
    void testSingleNormalInsertWritesContentToSpokeAndEnqueuesS3Write() {
        initSimpleContent(false);
        stubRealChannel();
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
        stubRealChannel();
        when(channelConfig.isBatch()).thenReturn(true);

        ccs.insert(channelName, content);
        verifyZeroInteractions(s3WriteQueue);
    }

    @Test
    @SneakyThrows
    void testLargePayloadWritesContentToS3AndIndexToSpokeAndS3() {
        initSimpleContent(true);
        stubRealChannel();
        when(channelConfig.isBatch()).thenReturn(false);

        ccs.insert(channelName, content);
        verify(mockS3LargeDao, times(1)).insert(channelName, content);
        verify(mockSpokeWriteDao, times(1)).insert(channelName, largeContentUtils.createIndex(content));
        verify(s3WriteQueue, times(1)).add(any());
    }

    @Test
    void testLatestImmutableIsEmptyIfChannelDoesntExist() {
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).build();
        when(contentRetriever.getCachedChannelConfig(channelName)).thenReturn(Optional.empty());
        Optional<ContentKey> key = ccs.getLatestImmutable(query);
        assertFalse(key.isPresent());
    }

    @Test
    void testLatestImmutableReturnsCachedItemIfQueryIsStable() {
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(true).build();
        stubRealChannel();
        ContentKey cachedKey = new ContentKey(TimeUtil.now().minusSeconds(5), "CacheLatest");
        when(latestContentCache.getLatest(channelName, null)).thenReturn(cachedKey);
        Optional<ContentKey> key = ccs.getLatestImmutable(query);
        assertTrue(key.isPresent());
        assertEquals(cachedKey, key.get());
    }

    @Test
    void testLatestImmutableFetchesLatestIfNoCacheExists() {
        ContentKey startKey = new ContentKey(TimeUtil.now().minusSeconds(20), "StartKey");
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(true).startKey(startKey).build();
        stubRealChannel();
        stubMissingCache();
        ContentKey spokeLatestKey = stubSpokeLatest(0, "SpokeLatest");
        Optional<ContentKey> key = ccs.getLatestImmutable(query);
        assertTrue(key.isPresent());
        assertEquals(spokeLatestKey, key.get());
    }

    @Test
    void testLatestImmutableIgnoresCachedItemIfQueryIsUnstable() {
        ContentKey startKey = new ContentKey(TimeUtil.now().minusSeconds(20), "StartKey");
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(false).startKey(startKey).build();
        stubRealChannel();
        stubCacheLatest(2, "CacheLatest");
        ContentKey spokeLatestKey = stubSpokeLatest(6, "SpokeLatest");
        Optional<ContentKey> key = ccs.getLatestImmutable(query);
        assertTrue(key.isPresent());
        assertEquals(spokeLatestKey, key.get());
    }

    @Test
    void testFindLatestKeyFindsSpokeKey() {
        ContentKey startKey = new ContentKey(TimeUtil.now().minusSeconds(20), "StartKey");
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(false).startKey(startKey).build();
        stubRealChannel();
        ContentKey cachedLatestKey = stubCacheLatest(6, "CacheLatest");
        ContentKey spokeLatestKey = stubSpokeLatest(1, "SpokeLatest");
        stubLongTermLatest(30, "LongTermLatest");
        Optional<ContentKey> latest = ccs.findLatestKey(query, channelName, Optional.of(cachedLatestKey));
        assertTrue(latest.isPresent());
        assertEquals(spokeLatestKey, latest.get());
        verifyZeroInteractions(contentRetriever);
    }

    @Test
    void testFindLatestKeyUsesCachedKeyIfSpokeIsEmpty() {
        ContentKey startKey = new ContentKey(TimeUtil.now().minusSeconds(20), "StartKey");
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(false).startKey(startKey).build();
        stubRealChannel();
        ContentKey cachedLatestKey = stubCacheLatest(6, "CacheLatest");
        stubLongTermLatest(30, "LongTermLatest");
        Optional<ContentKey> latest = ccs.findLatestKey(query, channelName, Optional.of(cachedLatestKey));
        assertTrue(latest.isPresent());
        assertEquals(cachedLatestKey, latest.get());
        verifyZeroInteractions(contentRetriever);
    }

    @Test
    void testFindLatestUsesSpokeKeyEvenIfItsOlderThanS3() {
        // Well...this IS existing behavior.  There're assumptions all over spoke that if it has content, it's newer than S3's content.
        ContentKey startKey = new ContentKey(TimeUtil.now().minusSeconds(20), "StartKey");
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(false).startKey(startKey).build();
        stubRealChannel();
        ContentKey spokeLatestKey = stubSpokeLatest(15, "SpokeLatest");
        ContentKey longTermLatest = stubLongTermLatest(3, "LongTermLatest");
        Optional<ContentKey> latest = ccs.findLatestKey(query, channelName, Optional.empty());
        assertTrue(latest.isPresent());
        assertEquals(spokeLatestKey, latest.get());
    }

    @Test
    void testFindLatestKeyUsesAndCachesS3KeyIfSpokeAndCacheAreEmpty() {
        ContentKey startKey = new ContentKey(TimeUtil.now().minusSeconds(20), "StartKey");
        DirectionQuery query = DirectionQuery.builder().channelName(channelName).stable(false).startKey(startKey).build();
        stubRealChannel();
        ContentKey longTermLatest = stubLongTermLatest(30, "LongTermLatest");
        Optional<ContentKey> latest = ccs.findLatestKey(query, channelName, Optional.empty());
        assertTrue(latest.isPresent());
        assertEquals(longTermLatest, latest.get());
    }

    private void stubRealChannel() {
        when(contentRetriever.getCachedChannelConfig(channelName)).thenReturn(Optional.of(channelConfig));
    }

    private ContentKey stubSpokeLatest(int ageInSeconds, String keyName) {
        ContentKey spokeLatestKey = new ContentKey(TimeUtil.now().minusSeconds(ageInSeconds), keyName);
        when(mockSpokeWriteDao.getLatest(eq(channelName), any(ContentKey.class), any(Traces.class))).thenReturn(Optional.of(spokeLatestKey));
        return spokeLatestKey;
    }

    private void stubMissingCache() {
        when(latestContentCache.getLatest(channelName, null)).thenReturn(null);
    }

    private ContentKey stubCacheLatest(int ageInSeconds, String keyName) {
        ContentKey cachedKey = new ContentKey(TimeUtil.now().minusSeconds(ageInSeconds), keyName);
        when(latestContentCache.getLatest(channelName, null)).thenReturn(cachedKey);
        return cachedKey;
    }

    private ContentKey stubLongTermLatest(int ageInSeconds, String keyName) {
        ContentKey longTermLatestKey = new ContentKey(TimeUtil.now().minusSeconds(ageInSeconds), keyName);
        when(contentRetriever
                .query(any(DirectionQuery.class)))
                .thenReturn(new TreeSet<>(Collections.singletonList(longTermLatestKey)));
        return longTermLatestKey;
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