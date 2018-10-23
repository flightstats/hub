package com.flightstats.hub.webhook;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebhookTest {

    @Test
    public void testChangeChannelUrlWithSameChannelName() {
        Webhook original = Webhook.builder()
                .name("name")
                .channelUrl("http://some.hub/channel/name")
                .build();

        Webhook proposed = Webhook.builder()
                .name("name")
                .channelUrl("http://different.hub/channel/name")
                .build();

        assertTrue(original.allowedToChange(proposed));
        assertTrue(original.isChanged(proposed));
    }

    @Test
    public void testChangeChannelUrlWithDifferentChannelName() {
        Webhook original = Webhook.builder()
                .name("name")
                .channelUrl("http://some.hub/channel/name")
                .build();

        Webhook proposed = Webhook.builder()
                .name("name")
                .channelUrl("http://some.hub/channel/differentName")
                .build();

        assertFalse(original.allowedToChange(proposed));
        assertTrue(original.isChanged(proposed));
    }

    @Test
    public void testChangeWebhookName() {
        Webhook original = Webhook.builder()
                .name("name")
                .channelUrl("http://hub/channel/name")
                .build();

        Webhook proposed = Webhook.builder()
                .name("differentName")
                .channelUrl("http://hub/channel/name")
                .build();

        assertFalse(original.allowedToChange(proposed));
        assertTrue(original.isChanged(proposed));
    }

    @Test
    public void testIsChangedParallelCalls() {
        Webhook original = Webhook.builder()
                .parallelCalls(1)
                .build();

        Webhook proposed = Webhook.builder()
                .parallelCalls(2)
                .build();

        assertTrue(original.isChanged(proposed));
    }

    @Test
    public void testIsTagPrototype() throws Exception {
        Webhook normal = Webhook.builder().build();
        assertFalse(normal.isTagPrototype());

        Webhook prototype = Webhook.builder().tagUrl("http://hub/tag/foo").build();
        assertTrue(prototype.isTagPrototype());
    }

}
