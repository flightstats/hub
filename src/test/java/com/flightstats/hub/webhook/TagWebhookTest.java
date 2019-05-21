package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.test.Integration;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

class TagWebhookTest {
    private static final String host = "http://hub.com";
    private ChannelConfig c1, c2, c3;
    private Set<Webhook> allWebhooks;

    @Mock
    private WebhookService webhookService;

    private String createChannelUrl(String channel) {
        return host + "/channel/" + channel;
    }

    private String createTagUrl(String tag) {
        return host + "/tag/" + tag;
    }

    private Webhook createWebhook(String name, String channel, String tag, boolean isInstance) {
        Webhook wh = Webhook.builder()
                .channelUrl(createChannelUrl(channel))
                .name(name)
                .build();

        if (isInstance) {
            return wh.withManagedByTag(tag);
        }
        return wh.withTagUrl(createTagUrl(tag));
    }

    private Set<Webhook> createWebhookSet() {
        Set<Webhook> result = new TreeSet<>();
        result.add(createWebhook("wh1", "c1", "", false));
        result.add(createWebhook("wh2", "c2", "t", true));
        result.add(createWebhook("wh3", "c3", "t", true));
        result.add(createWebhook("wh4", "c3", "t2", false));
        return result;
    }

    private ChannelConfig createChannelConfig(String name, String tag) {
        return ChannelConfig.builder()
                .name(name)
                .tags(StringUtils.isEmpty(tag) ? Collections.EMPTY_LIST : Collections.singletonList(tag))
                .build();
    }

    @BeforeEach
    void initialize() {
        try {
            Integration.startAwsHub();
        } catch (Exception e) {
            e.printStackTrace();
        }
        allWebhooks = createWebhookSet();
        c1 = createChannelConfig("c1", null);
        c2 = createChannelConfig("c2", "t");
        c3 = createChannelConfig("c3", "t");
    }

    @Test
    void testAllManagedWebhooksForChannel() {
        final TagWebhook tagWebhook = new TagWebhook(webhookService);

        Set<Webhook> webhooks = tagWebhook.allManagedWebhooksForChannel(allWebhooks, c2);
        assert webhooks.size() == 1 : "Should be only 1 tag webhook for c2";

        webhooks = tagWebhook.allManagedWebhooksForChannel(allWebhooks, c3);
        assert webhooks.size() == 1 : "c2 has one managed webhooks, the other is a prototype";

        webhooks = tagWebhook.allManagedWebhooksForChannel(allWebhooks, c1);
        assert webhooks.size() == 0 : "c1 has no tag webhooks";
    }

}