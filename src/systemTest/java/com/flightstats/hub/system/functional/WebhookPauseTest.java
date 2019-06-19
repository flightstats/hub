package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookType;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.extension.TestClassWrapper;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.WebhookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import java.util.List;
import java.util.Random;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookPauseTest extends TestClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private String webhookName;
    private String channelName;
    @Inject
    private ChannelService channelService;
    @Inject
    private WebhookService webhookService;
    @Inject
    private CallbackService callbackService;
    @Inject
    private ModelBuilder modelBuilder;

    private Webhook createWebhook(WebhookType type) {
        Webhook webhook = modelBuilder.webhookBuilder()
                .webhookName(webhookName)
                .channelName(channelName)
                .parallelCalls(3)
                .batchType(type)
                .build();
        webhookService.insertAndVerify(webhook);
        return webhook;
    }

    private void updatePauseOnWebhook(Webhook webhook, boolean pause) {
        Webhook updatedWebhook = modelBuilder.webhookBuilder()
                .webhookName(webhook.getName())
                .channelName(channelName)
                .isPaused(pause)
                .batchType(WebhookType.valueOf(webhook.getBatch()))
                .parallelCalls(1)
                .build();
        webhookService.updateAndVerify(updatedWebhook);
    }

    @BeforeEach
    void setup() {
        channelName = randomAlphaNumeric(10);
        webhookName = randomAlphaNumeric(10);
        channelService.createWithDefaults(channelName);
    }

    @AfterEach
    void cleanup() {
        channelService.delete(channelName);
        webhookService.delete(webhookName);
    }

    @ParameterizedTest
    @EnumSource(WebhookType.class)
    void webhookPause_webhookPauseAndUnPause_itemsAreCalledBackAfterUnPause(WebhookType type) {
        Webhook webhook = createWebhook(type);
        List<String> items = channelService.addItems(channelName, TEST_DATA, 5);
        updatePauseOnWebhook(webhook, true);
        Webhook pausedWebhook = webhookService.get(webhookName);
        assertTrue(pausedWebhook.isPaused());
        List<String> whilePausedItems = channelService.addItems(channelName, TEST_DATA, 5);
        assertTrue(callbackService.itemsNotSentToWebhook(webhookName, whilePausedItems, type),
                "items sent while webhook paused!");
        updatePauseOnWebhook(webhook, false);
        Webhook unpausedWebhook = webhookService.get(webhookName);
        assertFalse(unpausedWebhook.isPaused());
        List<String> unPausedItems = channelService.addItems(channelName, TEST_DATA, 5);
        unPausedItems.addAll(items);
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, unPausedItems));
    }
}
