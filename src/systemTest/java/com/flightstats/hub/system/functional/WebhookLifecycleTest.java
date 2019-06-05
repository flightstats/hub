package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import com.flightstats.hub.utility.StringHelper;

import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebhookLifecycleTest extends DependencyInjector {
    @Inject
    private CallbackService callbackResource;
    @Inject
    private ChannelService channelResource;
    @Inject
    private WebhookService webhookResource;
    private String channelName;
    private String webhookName;
    @Inject
    private HubLifecycle hubLifecycle;
    @Inject
    private ModelBuilder modelBuilder;
    @Inject
    private StringHelper stringHelper;

    @BeforeAll
    void hubSetup() {
        hubLifecycle.setup();
    }

    @BeforeEach
    void before() {
        this.channelName = stringHelper.randomAlphaNumeric(10);
        this.webhookName = stringHelper.randomAlphaNumeric(10);
    }

    private Webhook buildWebhook() {
        return modelBuilder.webhookBuilder()
                .channelName(channelName)
                .webhookName(webhookName)
                .build();
    }

    @Test
    @SneakyThrows
    void testWebhookWithNoStartItem() {
        final String data = "{\"fn\": \"first\", \"ln\":\"last\"}";

        channelResource.create(channelName);

        final Webhook webhook = buildWebhook().withParallelCalls(2);
        webhookResource.insertAndVerify(webhook);

        final List<String> channelItems = channelResource.addItems(channelName, data, 10);
        final List<String> channelItemsPosted = callbackResource.awaitItemCountSentToWebhook(webhookName, channelItems.size());

        Collections.sort(channelItems);
        Collections.sort(channelItemsPosted);
        assertEquals(channelItems, channelItemsPosted);
    }

    @Test
    @SneakyThrows
    void testWebhookWithStartItem() {
        final String data = "{\"key1\": \"value1\", \"key2\":\"value2\"}";

        channelResource.create(channelName);
        final List<String> channelItems = channelResource.addItems(channelName, data, 10);

        final Webhook webhook = buildWebhook().
                withStartItem(channelItems.get(4)).
                withParallelCalls(2);
        webhookResource.insertAndVerify(webhook);
        final List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        final List<String> channelItemsPosted = callbackResource.awaitItemCountSentToWebhook(webhookName, channelItemsExpected.size());

        Collections.sort(channelItemsExpected);
        Collections.sort(channelItemsPosted);
        assertEquals(channelItemsExpected, channelItemsPosted);
    }

    @Test
    @SneakyThrows
    void testWebhookWithStartItem_expectItemsInOrder() {
        final String data = "{\"city\": \"portland\", \"state\":\"or\"}";

        channelResource.create(channelName);
        final List<String> channelItems = channelResource.addItems(channelName, data, 10);

        final Webhook webhook = buildWebhook().
                withStartItem(channelItems.get(4)).
                withParallelCalls(1);
        webhookResource.insertAndVerify(webhook);
        final List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        final List<String> channelItemsPosted = callbackResource.awaitItemCountSentToWebhook(webhookName, channelItemsExpected.size());

        assertEquals(channelItemsExpected, channelItemsPosted);
    }

    @AfterEach
    @SneakyThrows
    void after() {
        this.channelResource.delete(channelName);
        this.webhookResource.delete(webhookName);
    }

    @AfterAll
    void hubCleanup() {
        hubLifecycle.cleanup();
    }
}
