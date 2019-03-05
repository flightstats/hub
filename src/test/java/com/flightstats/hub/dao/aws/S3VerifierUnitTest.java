package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.DistributedLeaderLockManager;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.sun.jersey.api.client.Client;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class S3VerifierUnitTest {

    @Test
    public void testZKDoesNotUpdateOnAbsoluteFailure() {
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ContentDao spokeWriteContentDao = mock(ContentDao.class);
        ContentDao s3SingleContentDao = mock(ContentDao.class);
        S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
        Client httpClient = mock(Client.class);
        DistributedLeaderLockManager lockManager = mock(DistributedLeaderLockManager.class);
        MetricsService metricsService = mock(MetricsService.class);
        S3Verifier s3Verifier = spy(new S3Verifier(lastContentPath, channelService, spokeWriteContentDao, s3SingleContentDao, s3WriteQueue, httpClient, lockManager, metricsService));

        ChannelContentKey key = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/bar");
        VerifierRange verifierRange = VerifierRange.builder()
                .channelConfig(ChannelConfig.builder().name("foo").build())
                .startPath(new MinutePath(DateTime.parse("1999-12-31T23:59:59.999Z")))
                .endPath(new MinutePath(DateTime.parse("2000-01-01T00:00:00.000Z")))
                .build();
        SortedSet<ContentKey> missingKeys = new TreeSet<>();
        missingKeys.add(key.getContentKey());

        when(s3Verifier.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo", s3SingleContentDao, new TreeSet<>())).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(false);

        s3Verifier.verifyChannel(verifierRange);

        verifyZeroInteractions(lastContentPath);
    }

    @Test
    public void testZKUpdatesWithPartialCompletionIfVerifierFailsPartwayThroughAndLastSuccessfulWasADifferentMinute() {
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ContentDao spokeWriteContentDao = mock(ContentDao.class);
        ContentDao s3SingleContentDao = mock(ContentDao.class);
        S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
        Client httpClient = mock(Client.class);
        DistributedLeaderLockManager lockManager = mock(DistributedLeaderLockManager.class);
        MetricsService metricsService = mock(MetricsService.class);
        S3Verifier s3Verifier = spy(new S3Verifier(lastContentPath, channelService, spokeWriteContentDao, s3SingleContentDao, s3WriteQueue, httpClient, lockManager, metricsService));

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

        when(s3Verifier.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo", s3SingleContentDao, new TreeSet<>())).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(true);
        when(s3WriteQueue.add(secondKey)).thenReturn(false);

        s3Verifier.verifyChannel(verifierRange);

        MinutePath firstKeyMinute = new MinutePath(key.getContentKey().getTime());
        verify(lastContentPath, times(1)).updateIncrease(firstKeyMinute, verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }

    @Test
    public void testZKUpdatesWithPartialCompletionIfVerifierFailsPartwayThroughAMinute() {
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ContentDao spokeWriteContentDao = mock(ContentDao.class);
        ContentDao s3SingleContentDao = mock(ContentDao.class);
        S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
        Client httpClient = mock(Client.class);
        DistributedLeaderLockManager lockManager = mock(DistributedLeaderLockManager.class);
        MetricsService metricsService = mock(MetricsService.class);
        S3Verifier s3Verifier = spy(new S3Verifier(lastContentPath, channelService, spokeWriteContentDao, s3SingleContentDao, s3WriteQueue, httpClient, lockManager, metricsService));

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

        when(s3Verifier.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo", s3SingleContentDao, new TreeSet<>())).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(true);
        when(s3WriteQueue.add(secondKey)).thenReturn(true);
        when(s3WriteQueue.add(thirdKey)).thenReturn(false);

        s3Verifier.verifyChannel(verifierRange);

        verify(s3WriteQueue, times(1)).add(key);
        verify(s3WriteQueue, times(1)).add(secondKey);
        verify(s3WriteQueue, times(1)).add(thirdKey);

        MinutePath minuteBeforeFailure = new MinutePath(DateTime.parse("1999-12-31T23:58:00.000Z"));
        verify(lastContentPath, times(1)).updateIncrease(minuteBeforeFailure, verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }

    @Test
    public void testZKUpdatedOnSuccess() {
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ContentDao spokeWriteContentDao = mock(ContentDao.class);
        ContentDao s3SingleContentDao = mock(ContentDao.class);
        S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
        Client httpClient = mock(Client.class);
        DistributedLeaderLockManager lockManager = mock(DistributedLeaderLockManager.class);
        MetricsService metricsService = mock(MetricsService.class);
        S3Verifier s3Verifier = spy(new S3Verifier(lastContentPath, channelService, spokeWriteContentDao, s3SingleContentDao, s3WriteQueue, httpClient, lockManager, metricsService));

        ChannelContentKey key = ChannelContentKey.fromResourcePath("http://hub/channel/foo/1999/12/31/23/59/59/999/bar");
        VerifierRange verifierRange = VerifierRange.builder()
                .channelConfig(ChannelConfig.builder().name("foo").build())
                .startPath(new MinutePath(DateTime.parse("1999-12-31T23:59:59.999Z")))
                .endPath(new MinutePath(DateTime.parse("2000-01-01T00:00:00.000Z")))
                .build();
        SortedSet<ContentKey> missingKeys = new TreeSet<>();
        missingKeys.add(key.getContentKey());

        when(s3Verifier.getMissing(verifierRange.getStartPath(), verifierRange.getEndPath(), "foo", s3SingleContentDao, new TreeSet<>())).thenReturn(missingKeys);
        when(s3WriteQueue.add(key)).thenReturn(true);

        s3Verifier.verifyChannel(verifierRange);

        verify(lastContentPath, times(1)).updateIncrease(verifierRange.getEndPath(), verifierRange.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
    }
}
