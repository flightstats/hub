package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.LocalChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Injector;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.flightstats.hub.dao.LocalChannelService.*;
import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class S3VerifierTest {
    private final static Logger logger = LoggerFactory.getLogger(S3VerifierTest.class);

    private static S3Verifier s3Verifier;
    private static LastContentPath lastContentPath;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private int offsetMinutes = 15;
    private static LocalChannelService localChannelService;

    @Rule
    public TestName testName = new TestName();
    private String channelName;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = Integration.startAwsHub();
        HubProperties.setProperty("spoke.ttlMinutes", "60");
        s3Verifier = injector.getInstance(S3Verifier.class);
        lastContentPath = injector.getInstance(LastContentPath.class);
        localChannelService = injector.getInstance(LocalChannelService.class);
    }

    @Before
    public void setUp() throws Exception {
        offsetTime = now.minusMinutes(offsetMinutes);
        channelName = testName.getMethodName() + RandomStringUtils.randomAlphanumeric(6);
    }

    @Test
    public void testSingleNormalDefault() {
        ChannelConfig channel = ChannelConfig.builder().withName(channelName).build();
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
        ChannelConfig channel = ChannelConfig.builder().withName(channelName).build();
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    @Test
    public void testSingleReplicatedDefault() {
        ChannelConfig channel = getReplicatedChannel(channelName);
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
        localChannelService.updateChannel(channel, null);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(offsetMinutes + 1)), range.startPath);
    }

    @Test
    public void testSingleNormalLagging() {
        MinutePath lastVerified = new MinutePath(now.minusMinutes(60));
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().withName(channelName).build();
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
        localChannelService.updateChannel(channel, null);
        S3Verifier.VerifierRange range = s3Verifier.getSingleVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.endPath);
        assertEquals(lastVerified, range.startPath);
    }

    @Test
    public void testHistoricalNone() {
        lastContentPath.initialize(channelName, ContentKey.NONE, HISTORICAL_FIRST_UPDATED);
        lastContentPath.initialize(channelName, ContentKey.NONE, HISTORICAL_LAST_UPDATED);
        ChannelConfig channel = getHistoricalChannel(channelName);
        localChannelService.updateChannel(channel, null);
        assertNull(s3Verifier.getHistoricalVerifierRange(now, channel));
    }

    @Test
    public void testHistoricalOneItem() {
        DateTime historyTime = now.minusYears(1);
        ContentKey oneKey = new ContentKey(historyTime);

        lastContentPath.update(oneKey, channelName, HISTORICAL_FIRST_UPDATED);
        lastContentPath.update(oneKey, channelName, HISTORICAL_LAST_UPDATED);
        ChannelConfig channel = getHistoricalChannel(channelName);
        localChannelService.updateChannel(channel, null);
        S3Verifier.VerifierRange range = s3Verifier.getHistoricalVerifierRange(now, channel);
        MinutePath expected = new MinutePath(historyTime);
        logger.info("{} {}", channelName, range);
        logger.info("{} expected {}", channelName, expected);

        assertEquals(expected, range.endPath);
        assertEquals(expected, range.startPath);

        lastContentPath.update(range.endPath, channelName, LAST_SINGLE_VERIFIED);
        range = s3Verifier.getHistoricalVerifierRange(now, channel);
        logger.info("{} {}", channelName, range);
        assertEquals(expected, range.endPath);
        assertEquals(expected, range.startPath);
    }

    @Test
    public void testHistoricalMultipleItems() {
        DateTime historyStart = now.minusYears(1);
        ContentKey firstKey = new ContentKey(historyStart);
        ContentKey lastKey = new ContentKey(historyStart.plusDays(1));

        lastContentPath.update(firstKey, channelName, HISTORICAL_FIRST_UPDATED);
        lastContentPath.update(lastKey, channelName, HISTORICAL_LAST_UPDATED);

        ChannelConfig channel = getHistoricalChannel(channelName);
        localChannelService.updateChannel(channel, null);
        S3Verifier.VerifierRange range = s3Verifier.getHistoricalVerifierRange(now, channel);


        logger.info("{} {}", channelName, range);
        logger.info("{} expected {}", channelName, new MinutePath(historyStart));

        assertEquals(new MinutePath(lastKey.getTime()), range.endPath);
        assertEquals(new MinutePath(firstKey.getTime()), range.startPath);

        logger.info("setting LAST_SINGLE_VERIFIED to endPath {}", range.endPath);
        lastContentPath.update(range.endPath, channelName, LAST_SINGLE_VERIFIED);
        range = s3Verifier.getHistoricalVerifierRange(now, channel);
        logger.info("range again {}", range);
        assertEquals(new MinutePath(lastKey.getTime()), range.endPath);
        assertEquals(new MinutePath(lastKey.getTime()), range.startPath);

        ContentKey nextLastKey = new ContentKey(lastKey.getTime().plusDays(1));
        lastContentPath.updateIncrease(nextLastKey, channelName, HISTORICAL_LAST_UPDATED);

        logger.info("before verifier LAST_SINGLE_VERIFIED {}", lastContentPath.getOrNull(channelName, LAST_SINGLE_VERIFIED));
        range = s3Verifier.getHistoricalVerifierRange(now, channel);
        logger.info("x2 testHistoricalMultipleItems {}", range);
        assertEquals(new MinutePath(nextLastKey.getTime()), range.endPath);
        assertEquals(new MinutePath(lastKey.getTime()), range.startPath);
    }

    private ChannelConfig getHistoricalChannel(String name) {
        return ChannelConfig.builder()
                .withName(name)
                .withHistorical(true)
                .withTtlDays(3650)
                .build();
    }

    private ChannelConfig getReplicatedChannel(String name) {
        return ChannelConfig.builder()
                .withName(name)
                .withReplicationSource("replicating")
                .build();
    }
}