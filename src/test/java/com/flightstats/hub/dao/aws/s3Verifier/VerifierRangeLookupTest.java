package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;
import java.util.Optional;


import static com.flightstats.hub.dao.ChannelService.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.dao.aws.S3Verifier.LAST_SINGLE_VERIFIED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class VerifierRangeLookupTest {
    private static VerifierRangeLookup verifierRangeLookup;
    private static LastContentPath lastContentPath;
    private static int ttlMinutes;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private int offsetMinutes = 15;
    private static ChannelService channelService;

    private String channelName;

    @BeforeAll
    static void setUpClass() throws Exception {
        final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
        Injector injector = Integration.startAwsHub();
        ttlMinutes =  spokeProperties.getTtlMinutes(SpokeStore.WRITE);
        verifierRangeLookup = injector.getInstance(VerifierRangeLookup.class);
        lastContentPath = injector.getInstance(LastContentPath.class);
        channelService = injector.getInstance(ChannelService.class);
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        offsetTime = now.minusMinutes(offsetMinutes);
        Optional<Method> currentTest = testInfo.getTestMethod();
        String nameBase = currentTest.isPresent() ? currentTest.get().getName() : "DEFAULTCHANNEL";
        channelName = (nameBase + StringUtils.randomAlphaNumeric(6)).toLowerCase();
        log.info("channel name " + channelName);
    }

    @Test
    void testSingleNormalDefault() {
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
    void testSingleNormal() {
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
    void testSingleReplicatedDefault() {
        ChannelConfig channel = getReplicatedChannel(channelName);
        channelService.createChannel(channel);
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        log.info("{} {}", channelName, range);
        assertEquals(new MinutePath(now.minusMinutes(1)), range.getEndPath());
        assertEquals(new MinutePath(range.getEndPath().getTime().minusMinutes(offsetMinutes)), range.getStartPath());
    }

    @Test
    void testSingleReplicated() {
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
    void testSingleNormalLagging() {
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
    void testSingleReplicationLagging() {
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
