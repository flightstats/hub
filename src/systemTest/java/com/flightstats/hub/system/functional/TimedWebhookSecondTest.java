package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import com.flightstats.hub.utility.StringHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.flightstats.hub.model.WebhookType.SECOND;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TimedWebhookSecondTest extends DependencyInjector {
    @Inject
    @Named("test.data")
    private String testData;
    private static final int CHANNEL_COUNT = 10;
    private static final int TIMEOUT = 5 * 60;

    private final List<String> channels = new ArrayList<>();
    private final List<Webhook> webhooks = new ArrayList<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> channelItemsInWebhook = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> channelItemsPosted = new ConcurrentHashMap<>();
    @Inject
    private StringHelper stringHelper;
    @Inject
    private ChannelService channelService;
    @Inject
    private WebhookService webhookService;
    @Inject
    private CallbackService callbackService;

    private void channelAndWebhookFactory() {
        for (int i = 0; i <= CHANNEL_COUNT; i++) {
            String channelName = stringHelper.randomAlphaNumeric(10);
            String webhookName = stringHelper.randomAlphaNumeric(10);
            channelService.create(channelName);
            Webhook webhook = Webhook.builder()
                    .channelUrl(channelService.getChannelUrl(channelName))
                    .name(webhookName)
                    .callbackUrl(callbackService.getCallbackUrl(webhookName))
                    .heartbeat(true)
                    .callbackTimeoutSeconds(TIMEOUT)
                    .batch(SECOND.name())
                    .build();
            webhookService.insertAndVerify(webhook);
            webhooks.add(webhook);
            channels.add(channelName);
        }

    }


    @BeforeEach
    @SneakyThrows
    void before() {
        channelAndWebhookFactory();
        for (int i = 0; i <= 5; i++) {
            channels.parallelStream().forEach(channelName -> {
                List<String> nextItems = channelService.addItems(channelName, testData, CHANNEL_COUNT / 2);
                ConcurrentLinkedQueue<String> items = new ConcurrentLinkedQueue<>();
                channelItemsPosted.putIfAbsent(channelName, items);
                nextItems.forEach(item -> channelItemsPosted.get(channelName).add(item));
            });
        }
    }

    @AfterEach
    void cleanup() {
        channels.forEach(channelService::delete);
        webhooks.forEach(webhook -> webhookService.delete(webhook.getName()));
    }

    private String getChannelName(Webhook webhook) {
        try {
            List<String> channelPath = Arrays.asList(webhook.getChannelUrl().split("/"));
            return channelPath.get(channelPath.indexOf("channel") + 1);
        } catch (Exception e) {
            log.error("failed to find channel name in webhook config");
            return "";
        }
    }

    private int getItemsPostedCount(String channelName) {
        try {
            return channelItemsPosted.get(channelName).size();
        } catch (Exception e) {
            log.error("failed to get count of items posted for channel: {}", channelName);
            throw e;
        }
    }

    @Test
    void timedWebhookSecondBatch_hasExpectedItems_items() {
        webhooks.parallelStream().forEach(webhook -> {
            String webhookName = webhook.getName();
            String channelName = getChannelName(webhook);
            int itemsPostedCount = getItemsPostedCount(channelName);
            ConcurrentLinkedQueue<String> items = new ConcurrentLinkedQueue<>();
            channelItemsInWebhook.putIfAbsent(channelName, items);
            List<String> nextItems = callbackService.awaitItemCountSentToWebhook(webhookName, itemsPostedCount);
            nextItems.forEach(item -> channelItemsInWebhook.get(channelName).add(item));
        });
        channelItemsPosted.forEach(4, (channelName, items) -> {
//            log.info("**** channel: {} items count: {}", channelName, items.size());
            boolean matched = items.parallelStream().allMatch(item -> {
                boolean hasItem = channelItemsInWebhook.get(channelName).contains(item);
                if (!hasItem) {
                    log.error("channelName {} item missing from webhook {}", channelName, item);
                }
                return hasItem;
            });
            assertTrue(matched);
        });
    }
}
