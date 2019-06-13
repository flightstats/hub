package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.flightstats.hub.constant.ZookeeperNodes.LAST_SINGLE_VERIFIED;
import static com.flightstats.hub.constant.ZookeeperNodes.REPLICATED_LAST_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerifierRangeLookupTest {
    private VerifierRangeLookup verifierRangeLookup;
    private ClusterCacheDao clusterCacheDao;
    private int ttlMinutes = 60;
    private DateTime now = TimeUtil.now();
    private DateTime offsetTime;
    private int offsetMinutes = 15;
    private ChannelService channelService;
    private String channelName;

    @BeforeAll
    void setUpClass() {
        IntegrationTestSetup integrationTestSetup = IntegrationTestSetup.run();
        verifierRangeLookup = integrationTestSetup.getInstance(VerifierRangeLookup.class);
        clusterCacheDao = integrationTestSetup.getInstance(ClusterCacheDao.class);
        channelService = integrationTestSetup.getInstance(ChannelService.class);
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
        clusterCacheDao.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
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
        clusterCacheDao.initialize(channelName, lastReplicated, REPLICATED_LAST_UPDATED);
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
        clusterCacheDao.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
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
        clusterCacheDao.initialize(channelName, lastReplicated, REPLICATED_LAST_UPDATED);
        MinutePath lastVerified = new MinutePath(now.minusMinutes(ttlMinutes + 2));
        clusterCacheDao.initialize(channelName, lastVerified, LAST_SINGLE_VERIFIED);
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
