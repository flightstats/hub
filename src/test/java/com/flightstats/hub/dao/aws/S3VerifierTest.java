package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.TimeUtil;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class S3VerifierTest {
    private final static Logger logger = LoggerFactory.getLogger(S3VerifierTest.class);

    private S3Verifier s3Verifier;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private MinutePath offsetPath;
    private LastContentPath lastContentPath;
    private int offsetMinutes = 15;

    @Before
    public void setUp() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        s3Verifier = new S3Verifier();
        lastContentPath = new LastContentPath(curator);
        s3Verifier.lastContentPath = lastContentPath;
        offsetTime = now.minusMinutes(offsetMinutes);
        offsetPath = new MinutePath(offsetTime);
        HubProperties.setProperty("spoke.ttlMinutes", "60");
    }

    @Test
    public void testSingleNormalDefault() {
        ChannelConfig channel = ChannelConfig.builder().withName("testSingleNormalDefault").build();
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleNormalDefault {}", range);
        assertEquals(channel, range.channel);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(offsetTime.minusMinutes(1)), range.startPath);
        assertEquals(range.endPath, new MinutePath(range.startPath.getTime().plusMinutes(offsetMinutes)));
    }

    @Test
    public void testSingleNormal() {
        MinutePath lastVerified = new MinutePath(offsetTime);
        lastContentPath.initialize("testSingleNormal", lastVerified, S3Verifier.LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testSingleNormal").build();
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleNormal {}", range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    @Test
    public void testSingleReplicatedDefault() {
        ChannelConfig channel = getReplicatedChannel("testSingleReplicatedDefault");
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleReplicatedDefault {}", range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(range.endPath.getTime().minusMinutes(offsetMinutes)), range.startPath);
    }

    @Test
    public void testSingleReplicated() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(30));
        lastContentPath.initialize("testSingleReplicated", lastReplicated, Replicator.REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel("testSingleReplicated");
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleReplicated {}", range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(offsetMinutes + 1)), range.startPath);
    }

    @Test
    public void testSingleNormalLagging() {
        MinutePath lastVerified = new MinutePath(now.minusMinutes(60));
        lastContentPath.initialize("testSingleNormalLagging", lastVerified, S3Verifier.LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testSingleNormalLagging").build();
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleNormalLagging {}", range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(now.minusMinutes(58)), range.startPath);
    }

    @Test
    public void testSingleReplicationLagging() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(90));
        lastContentPath.initialize("testSingleReplicationLagging", lastReplicated, Replicator.REPLICATED_LAST_UPDATED);
        MinutePath lastVerified = new MinutePath(now.minusMinutes(100));
        lastContentPath.initialize("testSingleReplicationLagging", lastVerified, S3Verifier.LAST_SINGLE_VERIFIED);
        ChannelConfig channel = getReplicatedChannel("testSingleReplicationLagging");
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleReplicationLagging {}", range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    @Test
    public void testBatchNormalDefault() {
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormalDefault").build();
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchNormalDefault {}", range);
        assertEquals(channel, range.channel);
        assertEquals(offsetPath, range.endPath);
        assertEquals(offsetPath, range.startPath);
    }

    @Test
    public void testBatchNormal() {
        MinutePath lastVerified = new MinutePath(offsetTime.minusMinutes(1));
        lastContentPath.initialize("testBatchNormal", lastVerified, S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormal").build();
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchNormal {}", range);
        assertEquals(offsetPath, range.endPath);
        assertEquals(offsetPath, range.startPath);
    }

    @Test
    public void testBatchNormalLagging() {
        MinutePath lastVerified = new MinutePath(offsetTime.minusMinutes(5));
        lastContentPath.initialize("testBatchNormalLagging", lastVerified, S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormalLagging").build();
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchNormalLagging {}", range);
        assertEquals(offsetPath, range.endPath);
        assertEquals(lastVerified.addMinute(), range.startPath);
    }

    @Test
    public void testBatchNormalSpoke() {
        lastContentPath.initialize("testBatchNormalSpoke", new MinutePath(now.minusMinutes(59)), S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testBatchNormalSpoke").build();
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchNormalSpoke {}", range);
        assertEquals(offsetPath, range.endPath);
        assertEquals(new MinutePath(now.minusMinutes(58)), range.startPath);
    }

    @Test
    public void testBatchReplicationDefault() {
        ChannelConfig channel = getReplicatedChannel("testBatchReplicationDefault");
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchReplicationDefault {}", range);
        assertEquals(offsetPath, range.endPath);
        assertEquals(offsetPath, range.startPath);
    }

    @Test
    public void testBatchReplicationLagging() {
        MinutePath endPath = new MinutePath(now.minusMinutes(10));
        lastContentPath.initialize("testBatchReplicationLagging", endPath, Replicator.REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel("testBatchReplicationLagging");
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchReplicationLagging {}", range);
        MinutePath expected = new MinutePath(now.minusMinutes(25));
        assertEquals(expected, range.endPath);
        assertEquals(expected, range.startPath);
    }

    @Test
    public void testBatchReplicationBehindSpoke() {
        MinutePath endPath = new MinutePath(now.minusMinutes(120));
        lastContentPath.initialize("testBatchReplicationBehindSpoke", endPath, Replicator.REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel("testBatchReplicationBehindSpoke");
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchReplicationBehindSpoke {}", range);
        MinutePath expected = new MinutePath(endPath.getTime().minusMinutes(15));
        assertEquals(expected, range.endPath);
        assertEquals(expected, range.startPath);
    }

    @Test
    public void testBatchReplBehindSpokeLagging() {
        MinutePath endPath = new MinutePath(now.minusMinutes(120));
        lastContentPath.initialize("testBatchReplBehindSpokeLagging", endPath, Replicator.REPLICATED_LAST_UPDATED);
        MinutePath lastPath = new MinutePath(now.minusMinutes(140));
        lastContentPath.initialize("testBatchReplBehindSpokeLagging", lastPath, S3Verifier.LAST_BATCH_VERIFIED);
        ChannelConfig channel = getReplicatedChannel("testBatchReplBehindSpokeLagging");
        S3Verifier.VerifierRange range = s3Verifier.getBatchVerifierRange(now, channel);
        logger.info("testBatchReplBehindSpokeLagging {}", range);
        MinutePath expected = new MinutePath(endPath.getTime().minusMinutes(15));
        assertEquals(expected, range.endPath);
        assertEquals(lastPath.addMinute(), range.startPath);
    }

    private ChannelConfig getReplicatedChannel(String name) {
        return ChannelConfig.builder()
                .withName(name)
                .withReplicationSource("replicating")
                .build();
    }
}