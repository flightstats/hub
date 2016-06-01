package com.flightstats.hub.util;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import static org.junit.Assert.*;

public class HubUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(HubUtilsTest.class);

    private static final String HUT_TEST = "hut_test";
    private HubUtils hubUtils;
    private String hubUrl;

    @Before
    public void setUp() throws Exception {
        Integration.startAwsHub();
        hubUtils = HubProvider.getInstance(HubUtils.class);
        hubUrl = "http://localhost:9080/";
    }

    @Test
    public void testCreateInsert() {
        String channelUrl = create();

        ChannelConfig channel = hubUtils.getChannel(channelUrl);
        assertNotNull(channel);
        assertEquals(HUT_TEST, channel.getName());
        assertEquals(120, channel.getTtlDays());

        String data = "some data " + System.currentTimeMillis();
        ByteArrayInputStream stream = new ByteArrayInputStream(data.getBytes());
        Content content = Content.builder()
                .withContentType("text/plain")
                .withStream(stream)
                .build();
        ContentKey key = hubUtils.insert(channelUrl, content);

        logger.info("key {}", key);
        assertNotNull(key);

        Content gotContent = hubUtils.get(channelUrl, key);
        assertNotNull(gotContent);
        assertEquals("text/plain", gotContent.getContentType().get());
        assertArrayEquals(data.getBytes(), gotContent.getData());
    }

    private String create() {
        ChannelConfig hut_test = ChannelConfig.builder().withName(HUT_TEST).build();
        String channelUrl = hubUrl + "channel/" + HUT_TEST;
        hubUtils.putChannel(channelUrl, hut_test);
        return channelUrl;
    }

    @Test
    public void testBulkInsert() {
        String channelUrl = create();
        String data = "--abcdefg\r\n" +
                "Content-Type: text/plain\r\n" +
                " \r\n" +
                "message one\r\n" +
                "--abcdefg\r\n" +
                "Content-Type: text/plain\r\n" +
                " \r\n" +
                "message two\r\n" +
                "--abcdefg\r\n" +
                "Content-Type: text/plain\r\n" +
                " \r\n" +
                "message three\r\n" +
                "--abcdefg\r\n" +
                "Content-Type: text/plain\r\n" +
                " \r\n" +
                "message four\r\n" +
                "--abcdefg--";
        ByteArrayInputStream stream = new ByteArrayInputStream(data.getBytes());
        BulkContent bulkContent = BulkContent.builder()
                .channel(HUT_TEST)
                .contentType("multipart/mixed; boundary=abcdefg")
                .stream(stream)
                .build();
        Collection<ContentKey> keys = hubUtils.insert(channelUrl + "/bulk", bulkContent);
        assertEquals(4, keys.size());
        for (ContentKey key : keys) {
            Content gotContent = hubUtils.get(channelUrl, key);
            assertNotNull(gotContent);
        }
    }
}
