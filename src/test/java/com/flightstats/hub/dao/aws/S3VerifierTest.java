package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.TestMain;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Injector;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.flightstats.hub.dao.ChannelService.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.junit.Assert.assertEquals;

public class S3VerifierTest {
    private final static Logger logger = LoggerFactory.getLogger(S3VerifierTest.class);

    private static S3Verifier s3Verifier;
    private static LastContentPath lastContentPath;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private int offsetMinutes = 15;
    private static ChannelService channelService;

    @Rule
    public TestName testName = new TestName();
    private String channelName;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = TestMain.start();
        HubProperties hubProperties = injector.getInstance(HubProperties.class);
        hubProperties.setProperty("spoke.ttlMinutes", "60");
        s3Verifier = injector.getInstance(S3Verifier.class);
        lastContentPath = injector.getInstance(LastContentPath.class);
        channelService = injector.getInstance(ChannelService.class);
    }

    @Before
    public void setUp() throws Exception {
        offsetTime = now.minusMinutes(offsetMinutes);
        channelName = (testName.getMethodName() + StringUtils.randomAlphaNumeric(6)).toLowerCase();
        logger.info("channel name " + channelName);
    }

    @Test
    public void testSingleNormalDefault() {
        ChannelConfig channel = ChannelConfig.builder().name(channelName).build();
        channelService.createChannel(channel);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(channel, range.channel);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(offsetTime.minusMinutes(1)), range.startPath);
        assertEquals(range.endPath, new MinutePath(range.startPath.getTime().plusMinutes(offsetMinutes)));
    }

    @Test
    public void testSingleNormal() {
        MinutePath lastVerified = new MinutePath(offsetTime);
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().name(channelName).build();
        channelService.createChannel(channel);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    @Test
    public void testSingleReplicatedDefault() {
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.createChannel(channel);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(range.endPath.getTime().minusMinutes(offsetMinutes)), range.startPath);
    }

    @Test
    public void testSingleReplicated() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(30));
        lastContentPath.initialize(channelName, lastReplicated, REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.updateChannel(channel, null, false);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(offsetMinutes + 1)), range.startPath);
    }

    @Test
    public void testSingleNormalLagging() {
        MinutePath lastVerified = new MinutePath(now.minusMinutes(60));
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().name(channelName).build();
        channelService.createChannel(channel);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        logger.info("expected {} {}", new MinutePath(now.minusMinutes(58)), range.startPath);
        //assertEquals(new MinutePath(now.minusMinutes(58)), range.startPath);
    }

    @Test
    public void testSingleReplicationLagging() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(90));
        lastContentPath.initialize(channelName, lastReplicated, REPLICATED_LAST_UPDATED);
        MinutePath lastVerified = new MinutePath(now.minusMinutes(100));
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.updateChannel(channel, null, false);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    private ChannelConfig getReplicatedChannel(String name) {
        return ChannelConfig.builder()
                .name(name)
                .replicationSource("replicating")
                .build();
    }
}