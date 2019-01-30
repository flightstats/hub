package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.aws.s3verifier.MissingContentFinder;
import com.flightstats.hub.dao.aws.s3verifier.VerifierConfig;
import com.flightstats.hub.dao.aws.s3verifier.VerifierRange;
import com.flightstats.hub.dao.aws.s3verifier.VerifierRangeLookup;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.sun.jersey.api.client.Client;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class S3VerifierUnitTest {
    private final LastContentPath lastContentPath = mock(LastContentPath.class);
    private final ChannelService channelService = mock(ChannelService.class);
    private final S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
    private final Client httpClient = mock(Client.class);
    private final ZooKeeperState zooKeeperState = mock(ZooKeeperState.class);
    private final CuratorFramework curator = mock(CuratorFramework.class);
    private final MetricsService metricsService = mock(MetricsService.class);
    private final ExecutorService channelThreadPool = mock(ExecutorService.class);
    private final MissingContentFinder missingContentFinder = mock(MissingContentFinder.class);
    private final VerifierRangeLookup verifierRangeLookup = mock(VerifierRangeLookup.class);

    @Test
    public void testZKNotUpdatedOnFailure() {
        VerifierConfig config = VerifierConfig.builder().build();
        S3Verifier s3Verifier = new S3Verifier(lastContentPath, channelService, s3WriteQueue, httpClient, zooKeeperState, curator, metricsService, missingContentFinder, verifierRangeLookup, config, channelThreadPool);

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

        verifyZeroInteractions(lastContentPath);
    }

    @Test
    public void testZKUpdatedOnSuccess() {
        VerifierConfig config = VerifierConfig.builder().build();
        S3Verifier s3Verifier = new S3Verifier(lastContentPath, channelService, s3WriteQueue, httpClient, zooKeeperState, curator, metricsService, missingContentFinder, verifierRangeLookup, config, channelThreadPool);

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

        verify(lastContentPath, times(1)).updateIncrease(verifierRange.getEndPath(), verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }
}
