package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.ChannelItemWithBody;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookType;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.extension.TestSuiteClassWrapper;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.WebhookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import java.util.List;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookPauseTest extends TestSuiteClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private String webhookName;
    private String channelName;
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator channelItemCreator;
    @Inject
    private WebhookService webhookService;
    @Inject
    private CallbackService callbackService;
    @Inject
    private ModelBuilder modelBuilder;

    private Webhook createWebhook(WebhookType type, boolean paused) {
        Webhook webhook = modelBuilder.webhookBuilder()
                .webhookName(webhookName)
                .channelName(channelName)
                .parallelCalls(3)
                .isPaused(paused)
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
        channelConfigService.createWithDefaults(channelName);
    }

    @AfterEach
    void cleanup() {
        channelConfigService.delete(channelName);
        webhookService.delete(webhookName);
    }

    @ParameterizedTest
    @EnumSource(WebhookType.class)
    void webhookPause_webhookStartPaused_itemsCalledBackAfterUnpause(WebhookType type) {
        Webhook webhook = createWebhook(type, true);
        assertTrue(webhookService.get(webhookName).isPaused());
        List<String> whilePausedItems = addFiveItems();
        assertTrue(callbackService.itemsNotSentToWebhook(webhookName, whilePausedItems, type),
                "items sent while webhook paused!");

        updatePauseOnWebhook(webhook, false);
        assertFalse(webhookService.get(webhookName).isPaused());
        List<String> unpausedItems = addFiveItems();
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, unpausedItems));
    }

    @ParameterizedTest
    @EnumSource(WebhookType.class)
    void webhookPause_webhookStartUnpaused_itemsNotCalledBackAfterPause(WebhookType type) {
        Webhook webhook = createWebhook(type, false);
        assertFalse(webhookService.get(webhookName).isPaused());
        List<String> items = addFiveItems();
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, items));

        updatePauseOnWebhook(webhook, true);
        assertTrue(webhookService.get(webhookName).isPaused());
        List<String> whilePausedItems = addFiveItems();
        assertTrue(callbackService.itemsNotSentToWebhook(webhookName, whilePausedItems, type),
                "items sent while webhook paused!");

    }

    @ParameterizedTest
    @EnumSource(WebhookType.class)
    void webhookPause_webhookPauseAndUnPause_itemsNotCalledBackDuringPause(WebhookType type) {
        Webhook webhook = createWebhook(type, false);
        List<String> items = addFiveItems();
        updatePauseOnWebhook(webhook, true);
        Webhook pausedWebhook = webhookService.get(webhookName);
        assertTrue(pausedWebhook.isPaused());
        List<String> whilePausedItems = addFiveItems();
        assertTrue(callbackService.itemsNotSentToWebhook(webhookName, whilePausedItems, type),
                "items sent while webhook paused!");
        updatePauseOnWebhook(webhook, false);
        Webhook unpausedWebhook = webhookService.get(webhookName);
        assertFalse(unpausedWebhook.isPaused());
        List<String> unpausedItems = addFiveItems();
        unpausedItems.addAll(items);
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, unpausedItems));
    }

    private List<String> addFiveItems() {
        return channelItemCreator.addItems(channelName, TEST_DATA, 5).stream()
                .map(ChannelItemWithBody::getItemUrl)
                .collect(toList());

    }
}
