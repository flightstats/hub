package com.flightstats.hub.replication;

import com.flightstats.hub.dao.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    //todo - gfm - 10/30/14 -
    /*@BeforeClass
    public static void setupClass() throws Exception {

        Injector injector = Integration.startRealHub();
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
        Content content = Content.builder()
                .withData("data2".getBytes())
                .withContentLanguage("lang")
                .withContentType(MediaType.APPLICATION_JSON)
                .withUser("root")
                .build();
        channelService.insert(channel, content);
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
    public void testGetFirstContent() throws Exception {
        Optional<Content> optionalContent = channelUtils.getContentV1(channelUrl, 1000);
        assertTrue(optionalContent.isPresent());
        Content content = optionalContent.get();
        assertArrayEquals("data1".getBytes(), content.getData());
        assertTrue(new DateTime(content.getMillis()).isAfter(new DateTime().minusMinutes(5)));
        assertFalse(content.getContentLanguage().isPresent());
        assertFalse(content.getUser().isPresent());
        assertEquals(content.getContentType().get(), MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    public void testGetContentHeaders() throws Exception {
        Optional<Content> optionalContent = channelUtils.getContentV1(channelUrl, 1001);
        assertTrue(optionalContent.isPresent());
        Content content = optionalContent.get();
        assertArrayEquals("data2".getBytes(), content.getData());
        assertTrue(new DateTime(content.getMillis()).isAfter(new DateTime().minusMinutes(5)));
        assertEquals(content.getContentType().get(), MediaType.APPLICATION_JSON);
        assertEquals(content.getContentLanguage().get(), "lang");
        assertEquals(content.getUser().get(), "root");
    }

    @Test
    public void testGetContent() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
        Optional<Content> optionalContent = channelUtils.getContentV1(channelUrl, latestSequence.get());
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
    }*/

}
