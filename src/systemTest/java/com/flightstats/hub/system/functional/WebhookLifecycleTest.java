package com.flightstats.hub.system.functional;

import com.flightstats.hub.kubernetes.HubLifecycleSuiteExtension;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.system.config.DependencyInjectionResolver;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.config.GuiceInjectionExtension;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;


import com.google.inject.Injector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({ GuiceInjectionExtension.class, DependencyInjectionResolver.class, HubLifecycleSuiteExtension.class})
class WebhookLifecycleTest {
    private final CallbackService callbackResource;
    private final ChannelService channelResource;
    private final WebhookService webhookResource;
    private final ModelBuilder modelBuilder;

    private String channelName;
    private String webhookName;

    WebhookLifecycleTest(CallbackService callbackResource,
                         ChannelService channelResource,
                         WebhookService webhookResource,
                         ModelBuilder modelBuilder) {
        this.callbackResource = callbackResource;
        this.channelResource = channelResource;
        this.webhookResource = webhookResource;
        this.modelBuilder = modelBuilder;

    }

    @BeforeEach
    void before() {
        this.channelName = randomAlphaNumeric(10);
        this.webhookName = randomAlphaNumeric(10);
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
        fail();
        String data = "{\"fn\": \"first\", \"ln\":\"last\"}";

        channelResource.create(channelName);

        Webhook webhook = buildWebhook().withParallelCalls(2);
        webhookResource.insertAndVerify(webhook);

        List<String> channelItems = channelResource.addItems(channelName, data, 10);
        assertTrue(callbackResource.areItemsEventuallySentToWebhook(webhookName, channelItems));
    }

    @Test
    @SneakyThrows
    void testWebhookWithStartItem() {
        fail();
        String data = "{\"key1\": \"value1\", \"key2\":\"value2\"}";

        channelResource.create(channelName);
        List<String> channelItems = channelResource.addItems(channelName, data, 10);
        Webhook webhook = buildWebhook().withStartItem(channelItems.get(4)).withParallelCalls(2);
        webhookResource.insertAndVerify(webhook);
        List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        assertTrue(callbackResource.areItemsEventuallySentToWebhook(webhookName, channelItemsExpected));
    }

    @Test
    @SneakyThrows
    void testWebhookWithStartItem_expectItemsInOrder() {
        fail();
        String data = "{\"city\": \"portland\", \"state\":\"or\"}";

        channelResource.create(channelName);
        List<String> channelItems = channelResource.addItems(channelName, data, 10);
        Webhook webhook = buildWebhook().withStartItem(channelItems.get(4)).withParallelCalls(1);
        webhookResource.insertAndVerify(webhook);
        List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        assertTrue(callbackResource.areItemsEventuallySentToWebhook(webhookName, channelItemsExpected));
    }

    @AfterEach
    @SneakyThrows
    void after() {
        channelResource.delete(channelName);
        webhookResource.delete(webhookName);
    }
}
