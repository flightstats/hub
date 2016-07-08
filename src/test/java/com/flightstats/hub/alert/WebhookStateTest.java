package com.flightstats.hub.alert;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.webhook.WebhookStatus;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.flightstats.hub.test.SparkUtil.get;
import static com.flightstats.hub.test.SparkUtil.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WebhookStateTest {

    private String hubAppUrl = "http://localhost:4567/";

    @AfterClass
    public static void tearDown() throws Exception {
        stop();
    }

    @Test
    public void testParse() throws IOException {
        URL resource = WebhookStateTest.class.getResource("/group.json");
        String configString = IOUtils.toString(resource);
        get("/webhook/testParse", (req, res) -> configString);
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("testParse")
                .hubDomain(hubAppUrl)
                .build();
        WebhookStatus webhookStatus = WebhookState.getStatus(alertConfig);
        assertEquals(new ContentKey(2015, 5, 28, 17, 6, 42, 376, "zYSn90"), webhookStatus.getChannelLatest());
        assertEquals(new ContentKey(2015, 5, 28, 17, 0, 42, 376, "ABC"), webhookStatus.getLastCompleted());
        assertEquals("http://hub/channel/provider", webhookStatus.getWebhook().getChannelUrl());
    }

    @Test
    public void testParseNoLatest() throws IOException {
        URL resource = WebhookStateTest.class.getResource("/group-no-latest.json");
        String configString = IOUtils.toString(resource);
        get("/webhook/testParseNoLatest", (req, res) -> configString);
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("testParseNoLatest")
                .hubDomain(hubAppUrl)
                .build();
        WebhookStatus webhookStatus = WebhookState.getStatus(alertConfig);
        assertNull(webhookStatus.getChannelLatest());
        assertNull(webhookStatus.getLastCompleted());
        assertEquals("http://hub/channel/provider", webhookStatus.getWebhook().getChannelUrl());
    }

    @Test
    public void testNoGroup() throws IOException {
        get("/webhook/testParseNoLatest", (req, res) -> {
            res.status(404);
            return "";
        });
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("testNoGroup")
                .hubDomain(hubAppUrl)
                .build();
        WebhookStatus webhookStatus = WebhookState.getStatus(alertConfig);
        assertNull(webhookStatus);
    }
}