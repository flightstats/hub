package com.flightstats.hub.system.functional;

import com.flightstats.hub.kubernetes.HubLifecycleSuiteExtension;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.kubernetes.HubLifecycle;
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

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({ GuiceInjectionExtension.class, HubLifecycleSuiteExtension.class})
class WebhookLifecycleTest extends DependencyInjector {
    private CallbackService callbackResource;
    private ChannelService channelResource;
    private WebhookService webhookResource;
    private HubLifecycle hubLifecycle;
    private ModelBuilder modelBuilder;

    private String channelName;
    private String webhookName;

    @BeforeEach
    void before(Injector injector) {
        this.channelName = randomAlphaNumeric(10);
        this.webhookName = randomAlphaNumeric(10);
        this.callbackResource = injector.getInstance(CallbackService.class);
        this.channelResource = injector.getInstance(ChannelService.class);
        this.webhookResource = injector.getInstance(WebhookService.class);
        this.hubLifecycle = injector.getInstance(HubLifecycle.class);
        this.modelBuilder = injector.getInstance(ModelBuilder.class);
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

    @AfterAll
    void hubCleanup() {
        hubLifecycle.cleanup();
    }
}
