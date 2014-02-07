package com.flightstats.hub.replication;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import com.google.common.base.Optional;
import com.google.inject.Injector;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * This is an integration test that relies on http://hub.svc.dev/channel/testy1 being actively updated.
 * It could be changed to fire up the Hub locally and run against that, which may make more sense.
 */
public class ChannelUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(ChannelUtilsTest.class);

    private static String rootUrl;
    private static String channelUrl;
    private static String nonChannelUrl;
    private static ChannelUtils channelUtils;
    private static String channel;
    private static ChannelService channelService;

    @BeforeClass
    public static void setupClass() throws Exception {

        Injector injector = Integration.startHub();
        String bindPort = Integration.getProperties().getProperty("http.bind_port", "8080");
        channelUtils = injector.getInstance(ChannelUtils.class);
        channel = Integration.getRandomChannel();
        rootUrl = "http://localhost:" + bindPort + "/channel";
        logger.info("using rootUrl " + rootUrl);
        channelUrl = rootUrl + "/" + channel;
        nonChannelUrl = rootUrl + "/blahFoobar";

        channelService = injector.getInstance(ChannelService.class);
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channel).build();
        channelService.createChannel(configuration);
        channelService.insert(channel, Content.builder().withData("data1".getBytes()).build());
        channelService.insert(channel, Content.builder().withData("data2".getBytes()).build());
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        channelService.delete(channel);
    }

    @Test
    public void testGetLatestSequence() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
        assertTrue(latestSequence.isPresent());
        System.out.println("latest " + latestSequence.get());
        assertTrue(latestSequence.get() > 1000);
    }

    @Test
    public void testGetLatestSequenceNoChannel() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(nonChannelUrl);
        assertFalse(latestSequence.isPresent());
    }

    @Test
    public void testGetConfiguration() throws Exception {
        Optional<ChannelConfiguration> configuration = channelUtils.getConfiguration(channelUrl);
        assertTrue(configuration.isPresent());
        ChannelConfiguration config = configuration.get();
        assertEquals(channel, config.getName());
        assertEquals(TimeUnit.DAYS.toMillis(120), (long) config.getTtlMillis());
        assertEquals(120, config.getTtlDays());
        assertTrue(config.isSequence());
    }

    @Test
    public void testGetConfigurationMissing() throws Exception {
        Optional<ChannelConfiguration> configuration = channelUtils.getConfiguration(nonChannelUrl);
        assertFalse(configuration.isPresent());
    }

    @Test
    public void testGetContent() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
        Optional<Content> optionalContent = channelUtils.getContent(channelUrl, latestSequence.get());
        assertTrue(optionalContent.isPresent());
        Content content = optionalContent.get();
        assertTrue(content.getData().length > 0);
        assertTrue(new DateTime(content.getMillis()).isAfter(new DateTime().minusMinutes(5)));
    }

    @Test
    public void testGetCreationDate() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
        Optional<DateTime> optionalDate = channelUtils.getCreationDate(channelUrl, latestSequence.get());
        assertTrue(optionalDate.isPresent());
        DateTime dateTime = optionalDate.get();
        assertTrue(dateTime.isAfter(new DateTime().minusMinutes(5)));
    }

    @Test
    public void testGetChannels() throws Exception {
        Set<Channel> channels = channelUtils.getChannels(rootUrl);
        assertNotNull(channels);
        assertTrue(channels.size() > 0);
        assertTrue(channels.contains(new Channel(channel, channelUrl)));
    }

    @Test
    public void testGetChannelsSlash() throws Exception {
        Set<Channel> channels = channelUtils.getChannels(rootUrl + "/");
        assertNotNull(channels);
        assertTrue(channels.size() > 0);
    }

    @Test
    public void testNoChannels() throws Exception {
        Set<Channel> channels = channelUtils.getChannels("http://nothing.svc.dev/channel");
        assertNotNull(channels);
        assertTrue(channels.isEmpty());
    }

}
