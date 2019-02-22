package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class WebhookTest {

    private Webhook webhook;

    @Before
    public void setUp() {
        webhook = Webhook.builder()
                .channelUrl("url").callbackUrl("end").build();
    }

    @Test
    public void testSimple() {
        Webhook webhook = Webhook.fromJson(this.webhook.toJson());
        assertEquals("end", webhook.getCallbackUrl());
        assertEquals("url", webhook.getChannelUrl());
        Assert.assertNull(webhook.getName());
    }

    @Test
    public void testTag() {
        String tagUrl = "http://hub/tag/allTheThings";
        Webhook aWebhook = Webhook.builder()
                .tagUrl(tagUrl)
                .callbackUrl("end").build();
        Webhook webhook = Webhook.fromJson(aWebhook.toJson());
        assertEquals("allTheThings", webhook.getTagFromTagUrl());
        assertEquals(tagUrl, webhook.getTagUrl());
    }

    @Test
    public void testWithName() {
        Webhook webhook = this.webhook.withName("wither");
        webhook = Webhook.fromJson(webhook.toJson());
        assertEquals("end", webhook.getCallbackUrl());
        assertEquals("url", webhook.getChannelUrl());
        assertEquals("wither", webhook.getName());
    }

    @Test
    public void testFromJson() {
        Webhook cycled = Webhook.fromJson(webhook.toJson());
        assertEquals(webhook, cycled);
    }

    @Test
    public void testJsonStartItem() {
        ContentKey key = new ContentKey();
        String json = "{\"callbackUrl\":\"end\",\"channelUrl\":\"url\",\"startItem\":\"" +
                "http://hub/channel/stuff/" + key.toUrl() +
                "\"}";
        Webhook cycled = Webhook.fromJson(json);
        assertEquals(webhook, cycled);
        assertEquals(key, cycled.getStartingKey());
        String toJson = cycled.toJson();
        assertNotNull(toJson);
    }

    @Test
    public void testJsonContentPath() {
        MinutePath key = new MinutePath();
        String json = "{\"callbackUrl\":\"end\",\"channelUrl\":\"url\"," +
                "\"startItem\":\"http://hub/channel/stuff/" + key.toUrl() +
                "\"}";
        Webhook cycled = Webhook.fromJson(json);
        assertEquals(webhook, cycled);
        assertEquals(key, cycled.getStartingKey());
        String toJson = cycled.toJson();
        assertNotNull(toJson);
    }

    @Test
    public void testWithDefaults() {
        assertNull(webhook.getParallelCalls());
        assertNull(webhook.getBatch());
        webhook = webhook.withDefaults();
        assertEquals(1L, (long) webhook.getParallelCalls());
        assertEquals("SINGLE", webhook.getBatch());
    }

    @Test
    public void testAllowedToChange() {
        Webhook hubA = Webhook.builder().name("name")
                .channelUrl("http://hubA/channel/name")
                .callbackUrl("url").build().withDefaults();
        Webhook hubB = Webhook.builder().name("name")
                .channelUrl("http://hubB/channel/name")
                .callbackUrl("url").build().withDefaults();

        assertTrue(hubA.allowedToChange(hubB));

        assertTrue(hubA.isChanged(hubB));

    }

    @Test
    public void testChannelUrlChange() {
        Webhook hubA = Webhook.builder().name("name")
                .channelUrl("http://hubA/channel/name")
                .callbackUrl("url").build().withDefaults();

        Webhook hubC = Webhook.builder().name("name")
                .channelUrl("http://hubC/channel/nameC")
                .callbackUrl("url").build().withDefaults();

        assertFalse(hubA.allowedToChange(hubC));

        assertTrue(hubA.isChanged(hubC));

    }

    @Test
    public void testStartingKey() {
        Webhook withDefaultsA = this.webhook.withDefaults();
        Webhook withDefaultsB = this.webhook.withDefaults();
        assertEquals(withDefaultsA, withDefaultsB);
        Webhook withStartingKey = withDefaultsB.withStartingKey(new ContentKey());
        assertEquals(withDefaultsA, withStartingKey);
        assertFalse(withDefaultsA.isChanged(withDefaultsA));
        assertFalse(withDefaultsB.isChanged(withDefaultsB));
    }

    @Test
    public void testIsTagPrototype() {
        Webhook withDefaultsA = this.webhook.withDefaults();
        assertFalse(withDefaultsA.isTagPrototype());
        Webhook twh = Webhook.builder().name("name")
                .callbackUrl("url")
                .tagUrl("http://hub.com/tag/twh")
                .build().withDefaults();
        assertTrue(twh.isTagPrototype());
    }

    @Test
    public void testSecondaryMetricsReporting() {
        Webhook withDefaults = this.webhook.withDefaults();
        assertFalse(withDefaults.isSecondaryMetricsReporting());
        String json = "{ \"secondaryMetricsReporting\": \"true\" }";
        Webhook newWebhook = Webhook.fromJson(json, Optional.of(withDefaults));
        assertTrue(newWebhook.isSecondaryMetricsReporting());
    }
}
