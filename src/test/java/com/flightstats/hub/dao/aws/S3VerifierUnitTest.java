package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.StatsdReporter;
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

import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class S3VerifierUnitTest {

    @Test
    public void testZKNotUpdatedOnFailure() {
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ContentDao spokeWriteContentDao = mock(ContentDao.class);
        ContentDao s3SingleContentDao = mock(ContentDao.class);
        S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
        Client httpClient = mock(Client.class);
        ZooKeeperState zooKeeperState = mock(ZooKeeperState.class);
        CuratorFramework curator = mock(CuratorFramework.class);
        StatsdReporter statsdReporter = mock(StatsdReporter.class);
        S3Verifier s3Verifier = spy(new S3Verifier(lastContentPath, channelService, spokeWriteContentDao, s3SingleContentDao, s3WriteQueue, httpClient, zooKeeperState, curator, statsdReporter));

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
    public void testZKUpdatedOnSuccess() {
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ContentDao spokeWriteContentDao = mock(ContentDao.class);
        ContentDao s3SingleContentDao = mock(ContentDao.class);
        S3WriteQueue s3WriteQueue = mock(S3WriteQueue.class);
        Client httpClient = mock(Client.class);
        ZooKeeperState zooKeeperState = mock(ZooKeeperState.class);
        CuratorFramework curator = mock(CuratorFramework.class);
        StatsdReporter statsdReporter = mock(StatsdReporter.class);
        S3Verifier s3Verifier = spy(new S3Verifier(lastContentPath, channelService, spokeWriteContentDao, s3SingleContentDao, s3WriteQueue, httpClient, zooKeeperState, curator, statsdReporter));

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
