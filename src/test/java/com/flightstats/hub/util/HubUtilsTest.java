package com.flightstats.hub.util;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;

public class HubUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(HubUtilsTest.class);
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
        ChannelConfig hut_test = ChannelConfig.builder().withName("hut_test").build();
        String channelUrl = hubUrl + "channel/hut_test";
        hubUtils.putChannel(channelUrl, hut_test);

        ChannelConfig channel = hubUtils.getChannel(channelUrl);
        assertNotNull(channel);
        assertEquals("hut_test", channel.getName());
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
}
