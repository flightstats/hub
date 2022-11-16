package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.DistributedLeaderLockManager;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.s3Verifier.MissingContentFinder;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierConfig;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierRange;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierRangeLookup;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.sun.jersey.api.client.Client;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import static com.flightstats.hub.constant.ZookeeperNodes.LAST_SINGLE_VERIFIED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class S3VerifierUnitTest {
    private final ClusterCacheDao clusterCacheDao = mock(ClusterCacheDao.class);
    private final S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
    private final Client httpClient = mock(Client.class);
    private final ExecutorService channelThreadPool = mock(ExecutorService.class);
    private final MissingContentFinder missingContentFinder = mock(MissingContentFinder.class);
    private final VerifierRangeLookup verifierRangeLookup = mock(VerifierRangeLookup.class);
    private final DistributedLeaderLockManager lockManager = mock(DistributedLeaderLockManager.class);
    private final StatsdReporter statsdReporter = mock(StatsdReporter.class);
    private final ContentRetriever contentRetriever = mock(ContentRetriever.class);
    private final Dao<ChannelConfig> channelConfigDao = mock(Dao.class);

    private S3Verifier s3Verifier;

    @BeforeEach
    public void setup() {
        final VerifierConfig config = VerifierConfig.builder().build();
        s3Verifier = new S3Verifier(
                clusterCacheDao,
                s3WriteQueue,
                httpClient,
                missingContentFinder,
                contentRetriever,
                verifierRangeLookup,
                config,
                channelThreadPool,
                channelConfigDao,
                lockManager,
                statsdReporter);
    }

    @Test
    void testZKDoesNotUpdateOnAbsoluteFailure() {
        ChannelContentKey key = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/bar");
        VerifierRange verifierRange = VerifierRange.builder()
                .channelConfig(ChannelConfig.builder().name("foo").build())
                .startPath(new MinutePath(DateTime.parse("1999-12-31T23:59:59.999Z")))
                .endPath(new MinutePath(DateTime.parse("2000-01-01T00:00:00.000Z")))
                .build();
        SortedSet<ContentKey> missingKeys = new TreeSet<>();
        missingKeys.add(key.getContentKey());

        when(missingContentFinder.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo")).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(false);

        s3Verifier.verifyChannel(verifierRange);

        verifyNoMoreInteractions(clusterCacheDao);
    }

    @Test
    void testZKUpdatesWithPartialCompletionIfVerifierFailsPartwayThroughAndLastSuccessfulWasADifferentMinute() {
        ChannelContentKey key = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/58/59/999/bar");
        ChannelContentKey secondKey = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/bar");
        VerifierRange verifierRange = VerifierRange.builder()
                .channelConfig(ChannelConfig.builder().name("foo").build())
                .startPath(new MinutePath(DateTime.parse("1999-12-31T23:57:59.999Z")))
                .endPath(new MinutePath(DateTime.parse("2000-01-01T00:00:00.000Z")))
                .build();
        SortedSet<ContentKey> missingKeys = new TreeSet<>();
        missingKeys.add(secondKey.getContentKey());
        missingKeys.add(key.getContentKey());

        when(missingContentFinder.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo")).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(true);
        when(s3WriteQueue.add(secondKey)).thenReturn(false);

        s3Verifier.verifyChannel(verifierRange);

        MinutePath firstKeyMinute = new MinutePath(key.getContentKey().getTime());
        verify(clusterCacheDao, times(1)).setIfNewer(firstKeyMinute, verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }

    @Test
    void testZKUpdatesWithPartialCompletionIfVerifierFailsPartwayThroughAMinute() {
        ChannelContentKey key = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/57/59/999/bar");
        ChannelContentKey secondKey = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/bar");
        ChannelContentKey thirdKey = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/baz");
        VerifierRange verifierRange = VerifierRange.builder()
                .channelConfig(ChannelConfig.builder().name("foo").build())
                .startPath(new MinutePath(DateTime.parse("1999-12-31T23:56:59.999Z")))
                .endPath(new MinutePath(DateTime.parse("2000-01-01T00:00:00.000Z")))
                .build();
        SortedSet<ContentKey> missingKeys = new TreeSet<>();
        missingKeys.add(thirdKey.getContentKey());
        missingKeys.add(key.getContentKey());
        missingKeys.add(secondKey.getContentKey());

        when(missingContentFinder.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo")).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(true);
        when(s3WriteQueue.add(secondKey)).thenReturn(true);
        when(s3WriteQueue.add(thirdKey)).thenReturn(false);

        s3Verifier.verifyChannel(verifierRange);

        verify(s3WriteQueue, times(1)).add(key);
        verify(s3WriteQueue, times(1)).add(secondKey);
        verify(s3WriteQueue, times(1)).add(thirdKey);

        MinutePath minuteBeforeFailure = new MinutePath(DateTime.parse("1999-12-31T23:58:00.000Z"));
        verify(clusterCacheDao, times(1)).setIfNewer(minuteBeforeFailure, verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }

    @Test
    void testZKUpdatedOnSuccess() {
        ChannelContentKey key = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/bar");
        VerifierRange verifierRange = VerifierRange.builder()
                .channelConfig(ChannelConfig.builder().name("foo").build())
                .startPath(new MinutePath(DateTime.parse("1999-12-31T23:59:59.999Z")))
                .endPath(new MinutePath(DateTime.parse("2000-01-01T00:00:00.000Z")))
                .build();
        SortedSet<ContentKey> missingKeys = new TreeSet<>();
        missingKeys.add(key.getContentKey());

        when(missingContentFinder.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo")).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(true);

        s3Verifier.verifyChannel(verifierRange);

        verify(clusterCacheDao, times(1)).setIfNewer(verifierRange.getEndPath(), verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }
}
