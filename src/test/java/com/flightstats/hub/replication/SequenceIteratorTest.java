package com.flightstats.hub.replication;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 *
 */
public class SequenceIteratorTest {
    private final static Logger logger = LoggerFactory.getLogger(SequenceIteratorTest.class);

    private static String channelName;
    private static String channelUrl;
    private static ChannelService channelService;
    private static SequenceIteratorFactory factory;

    @BeforeClass
    public static void setupClass() throws Exception {

        Injector injector = Integration.startRealHub();
        String bindPort = Integration.getProperties().getProperty("http.bind_port", "8080");
        channelName = Integration.getRandomChannel();
        String rootUrl = "http://localhost:" + bindPort + "/channel";
        logger.info("using rootUrl " + rootUrl);
        channelUrl = rootUrl + "/" + channelName;

        channelService = injector.getInstance(ChannelService.class);
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).build();
        channelService.createChannel(configuration);
        insert("data1");
        insert("data2");

        factory = injector.getInstance(SequenceIteratorFactory.class);
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        channelService.delete(channelName);
    }

    private static void insert(String data) {
        channelService.insert(channelName, Content.builder().withData(data.getBytes()).build());
    }

    @Test
    public void testSimple() throws Exception {
        Channel channel = new Channel(channelName, channelUrl);
        SequenceIterator iterator = factory.create(999, channel);
        check(iterator, "data1");
        check(iterator, "data2");
        int end = insertAndCheck(iterator, 3);
        iterator.exit();
        iterator = factory.create(iterator.getCurrent() , channel);
        insertAndCheck(iterator, end );
    }

    private int insertAndCheck(SequenceIterator iterator, int start) {
        int end = start + 10;
        for (int i = start; i < end; i++) {
            String data = "data" + i;
            insert(data);
            check(iterator, data);
        }
        return end;
    }

    private void check(SequenceIterator iterator, String data) {
        assertTrue(iterator.hasNext());
        Content content = iterator.next();
        logger.debug("content.getData() " + new String(content.getData()) + " " + content.getContentKey().get());
        assertNotNull(content);
        assertArrayEquals(data.getBytes(), content.getData());
    }
}
