package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookType;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.extension.TestSuiteClassWrapper;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static junit.framework.Assert.assertTrue;

@Slf4j
class TimedWebhooksTest extends TestSuiteClassWrapper {
    private static final String TEST_DATA = "TEST_DATA";
    private static final int CHANNEL_COUNT = 5;
    private static final int TIMEOUT = 5 * 60;

    private final List<String> channels = new ArrayList<>();
    private final List<Webhook> webhooks = new ArrayList<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> channelItemsPosted = new ConcurrentHashMap<>();
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator itemCreator;
    @Inject
    private WebhookService webhookService;
    @Inject
    private CallbackService callbackService;
    @Inject
    private ModelBuilder modelBuilder;

    @AfterEach
    void cleanup() {
        channels.forEach(channelConfigService::delete);
        webhooks.forEach(webhook -> webhookService.delete(webhook.getName()));
        channels.clear();
        webhooks.clear();
        channelItemsPosted.clear();
    }

    private boolean channelAndWebhookFactory(WebhookType type) {
        try {
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                String channelName = randomAlphaNumeric(10);
                String webhookName = randomAlphaNumeric(10);
                channelConfigService.createWithDefaults(channelName);
                Webhook webhook = modelBuilder
                        .webhookBuilder()
                        .channelName(channelName)
                        .webhookName(webhookName)
                        .batchType(type)
                        .heartbeat(true)
                        .callbackTimeoutSeconds(TIMEOUT)
                        .build();
                webhookService.insertAndVerify(webhook);
                webhooks.add(webhook);
                channels.add(channelName);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }


    }

    private boolean addItems() {
        try {
            for (int i = 0; i <= 3; i++) {
                channels.parallelStream().forEach(channelName -> {
                    List<String> nextItems = itemCreator.addItems(channelName, TEST_DATA, CHANNEL_COUNT / 4);
                    ConcurrentLinkedQueue<String> items = new ConcurrentLinkedQueue<>();
                    channelItemsPosted.putIfAbsent(channelName, items);
                    nextItems.forEach(item -> channelItemsPosted.get(channelName).add(item));
                });
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    private List<String> getItemsPostedForChannel(String channelName) {
        try {
            return new ArrayList<>(channelItemsPosted.get(channelName));
        } catch (Exception e) {
            log.error("failed to get count of items posted for channel: {}", channelName);
            throw e;
        }
    }

    @ParameterizedTest
    @EnumSource(value = WebhookType.class, names = { "SECOND", "MINUTE" })
    void timedWebhookBatch_hasExpectedItems_items(WebhookType type) {
        Awaitility.await().atMost(Duration.TWO_MINUTES).until(() -> channelAndWebhookFactory(type));
        Awaitility.await().atMost(Duration.TWO_MINUTES).until(this::addItems);

        webhooks.parallelStream().forEach(webhook -> {
            String webhookName = webhook.getName();
            String channelName = webhookService.getChannelName(webhook);
            List<String> itemsPosted = getItemsPostedForChannel(channelName);
            assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, itemsPosted));
        });
    }
}
