package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.SpokeProperty;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static com.flightstats.hub.dao.ChannelService.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.junit.Assert.assertEquals;

@Slf4j
public class VerifierRangeLookupTest {

    private static final SpokeProperty spokeProperty = new SpokeProperty(PropertyLoader.getInstance());

    private static VerifierRangeLookup verifierRangeLookup;
    private static LastContentPath lastContentPath;
    private static int ttlMinutes;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private int offsetMinutes = 15;
    private static ChannelService channelService;

    @Rule
    public TestName testName = new TestName();
    private String channelName;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = Integration.startAwsHub();
        ttlMinutes =  spokeProperty.getTtlMinutes(SpokeStore.WRITE);
        verifierRangeLookup = injector.getInstance(VerifierRangeLookup.class);
        lastContentPath = injector.getInstance(LastContentPath.class);
        channelService = injector.getInstance(ChannelService.class);
    }

    @Before
    public void setUp() {
        offsetTime = now.minusMinutes(offsetMinutes);
        channelName = (testName.getMethodName() + StringUtils.randomAlphaNumeric(6)).toLowerCase();
        log.info("channel name " + channelName);
    }

    @Test
    public void testSingleNormalDefault() {
        ChannelConfig channelConfig = ChannelConfig.builder().name(channelName).build();
        channelService.createChannel(channelConfig);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channelConfig);
        log.info("{} {}", channelName, range);
        assertEquals(channelConfig, range.getChannelConfig());
        assertEquals(new MinutePath(now.minusMinutes(1)), range.getEndPath());
        assertEquals(new MinutePath(offsetTime.minusMinutes(1)), range.getStartPath());
        assertEquals(range.getEndPath(), new MinutePath(range.getStartPath().getTime().plusMinutes(offsetMinutes)));
    }

    @Test
    public void testSingleNormal() {
        MinutePath lastVerified = new MinutePath(offsetTime);
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().name(channelName).build();
        channelService.createChannel(channel);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        log.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.getEndPath());
        assertEquals(lastVerified, range.getStartPath());
    }

    @Test
    public void testSingleReplicatedDefault() {
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.createChannel(channel);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        log.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.getEndPath());
        assertEquals(new MinutePath(range.getEndPath().getTime().minusMinutes(offsetMinutes)), range.getStartPath());
    }

    @Test
    public void testSingleReplicated() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(30));
        lastContentPath.initialize(channelName, lastReplicated, REPLICATED_LAST_UPDATED);
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.updateChannel(channel, null, false);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        log.info("{} {}", channelName, range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.getEndPath());
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(offsetMinutes + 1)), range.getStartPath());
    }

    @Test
    public void testSingleNormalLagging() {
        MinutePath lastVerified = new MinutePath(now.minusMinutes(ttlMinutes));
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = ChannelConfig.builder().name(channelName).build();
        channelService.createChannel(channel);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        log.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.getEndPath());
        log.info("expected {} {}", new MinutePath(now.minusMinutes(58)), range.getStartPath());
        //assertEquals(new MinutePath(now.minusMinutes(58)), range.startPath);
    }

    @Test
    public void testSingleReplicationLagging() {
        MinutePath lastReplicated = new MinutePath(now.minusMinutes(ttlMinutes + 1));
        lastContentPath.initialize(channelName, lastReplicated, REPLICATED_LAST_UPDATED);
        MinutePath lastVerified = new MinutePath(now.minusMinutes(ttlMinutes + 2));
        lastContentPath.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.updateChannel(channel, null, false);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        log.info("{} {}", channelName, range);
        assertEquals(new MinutePath(lastReplicated.getTime().minusMinutes(1)), range.getEndPath());
        assertEquals(lastVerified, range.getStartPath());
    }

    private ChannelConfig getReplicatedChannel(String name) {
        return ChannelConfig.builder()
                .name(name)
                .replicationSource("replicating")
                .build();
    }
}
