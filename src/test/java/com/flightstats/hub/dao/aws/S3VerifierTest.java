package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.TimeUtil;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class S3VerifierTest {

    private S3Verifier s3Verifier;
    DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private MinutePath offsetPath;
    private LastContentPath lastContentPath;

    @Before
    public void setUp() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        s3Verifier = new S3Verifier();
        lastContentPath = new LastContentPath(curator);
        s3Verifier.lastContentPath = lastContentPath;
        offsetTime = now.minusMinutes(15);
        offsetPath = new MinutePath(offsetTime);
        HubProperties.setProperty("spoke.ttlMinutes", "60");
    }

    @Test
    public void testBatchNormalDefault() {
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormalDefault").build();
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        assertEquals(offsetPath, verifierRange.endPath);
        assertEquals(offsetPath, verifierRange.startPath);
    }

    @Test
    public void testBatchNormal() {
        MinutePath lastVerified = new MinutePath(offsetTime.minusMinutes(1));
        lastContentPath.initialize("testBatchNormal", lastVerified, S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormal").build();
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        assertEquals(offsetPath, verifierRange.endPath);
        assertEquals(offsetPath, verifierRange.startPath);
    }

    @Test
    public void testBatchNormalLagging() {
        MinutePath lastVerified = new MinutePath(offsetTime.minusMinutes(5));
        lastContentPath.initialize("testBatchNormalLagging", lastVerified, S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormalLagging").build();
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        assertEquals(offsetPath, verifierRange.endPath);
        assertEquals(lastVerified.addMinute(), verifierRange.startPath);
    }

    @Test
    public void testBatchNormalSpoke() {
        lastContentPath.initialize("testBatchNormalSpoke", new MinutePath(now.minusMinutes(59)), S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormalSpoke").build();
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        assertEquals(offsetPath, verifierRange.endPath);
        assertEquals(new MinutePath(now.minusMinutes(58)), verifierRange.startPath);
    }

    @Test
    public void testBatchReplicationDefault() {
        ChannelConfig channel = getReplicatedChannel("testBatchReplicationDefault");
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        assertEquals(offsetPath, verifierRange.endPath);
        assertEquals(offsetPath, verifierRange.startPath);
    }

    @Test
    public void testBatchReplicationLagging() {
        MinutePath endPath = new MinutePath(now.minusMinutes(10));
        lastContentPath.initialize("testBatchReplicationLagging", endPath, ChannelReplicator.REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel("testBatchReplicationLagging");
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        MinutePath expected = new MinutePath(now.minusMinutes(25));
        assertEquals(expected, verifierRange.endPath);
        assertEquals(expected, verifierRange.startPath);
    }

    @Test
    public void testBatchReplicationBehindSpoke() {
        MinutePath endPath = new MinutePath(now.minusMinutes(120));
        lastContentPath.initialize("testBatchReplicationBehindSpoke", endPath, ChannelReplicator.REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel("testBatchReplicationBehindSpoke");
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        MinutePath expected = new MinutePath(endPath.getTime().minusMinutes(15));
        assertEquals(expected, verifierRange.endPath);
        assertEquals(expected, verifierRange.startPath);
    }

    @Test
    public void testBatchReplBehindSpokeLagging() {
        MinutePath endPath = new MinutePath(now.minusMinutes(120));
        lastContentPath.initialize("testBatchReplBehindSpokeLagging", endPath, ChannelReplicator.REPLICATED_LAST_UPDATED);
        MinutePath lastPath = new MinutePath(now.minusMinutes(140));
        lastContentPath.initialize("testBatchReplBehindSpokeLagging", lastPath, S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = getReplicatedChannel("testBatchReplBehindSpokeLagging");
        S3Verifier.VerifierRange verifierRange = s3Verifier.getVerifierRange(now, channel);
        MinutePath expected = new MinutePath(endPath.getTime().minusMinutes(15));
        assertEquals(expected, verifierRange.endPath);
        assertEquals(lastPath.addMinute(), verifierRange.startPath);
    }

    private ChannelConfig getReplicatedChannel(String name) {
        return ChannelConfig.builder()
                .withName(name)
                .withReplicationSource("replicating")
                .build();
    }
}