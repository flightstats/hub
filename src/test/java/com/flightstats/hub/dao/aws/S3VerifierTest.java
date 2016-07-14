package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.LocalChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Injector;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.flightstats.hub.dao.LocalChannelService.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.junit.Assert.assertEquals;

public class S3VerifierTest {
    private final static Logger logger = LoggerFactory.getLogger(S3VerifierTest.class);

    private static S3Verifier s3Verifier;
    private static LastContentPath lastContentPath;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private int offsetMinutes = 15;
    private static LocalChannelService localChannelService;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HubProperties.setProperty("spoke.ttlMinutes", "60");
        Injector injector = Integration.startAwsHub();
        s3Verifier = injector.getInstance(S3Verifier.class);
        lastContentPath = injector.getInstance(LastContentPath.class);
        localChannelService = injector.getInstance(LocalChannelService.class);
    }

    @Before
    public void setUp() throws Exception {
        offsetTime = now.minusMinutes(offsetMinutes);
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
        lastContentPath.initialize("testSingleNormal", lastVerified, LAST_SINGLE_VERIFIED);
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
        lastContentPath.initialize("testSingleReplicated", lastReplicated, REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel("testSingleReplicated");
        localChannelService.createChannel(channel);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleReplicated {}", range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(offsetMinutes + 1)), range.startPath);
    }

    @Test
    public void testSingleNormalLagging() {
        MinutePath lastVerified = new MinutePath(now.minusMinutes(60));
        lastContentPath.initialize("testSingleNormalLagging", lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName("testSingleNormalLagging").build();
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleNormalLagging {}", range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        logger.info("expected {} {}", new MinutePath(now.minusMinutes(58)), range.startPath);
        //assertEquals(new MinutePath(now.minusMinutes(58)), range.startPath);
    }

    @Test
    public void testSingleReplicationLagging() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(90));
        lastContentPath.initialize("testSingleReplicationLagging", lastReplicated, REPLICATED_LAST_UPDATED);
        MinutePath lastVerified = new MinutePath(now.minusMinutes(100));
        lastContentPath.initialize("testSingleReplicationLagging", lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = getReplicatedChannel("testSingleReplicationLagging");
        localChannelService.createChannel(channel);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("testSingleReplicationLagging {}", range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    //todo - gfm - 7/14/16 - tests for historical

    private ChannelConfig getReplicatedChannel(String name) {
        return ChannelConfig.builder()
                .withName(name)
                .withReplicationSource("replicating")
                .build();
    }
}