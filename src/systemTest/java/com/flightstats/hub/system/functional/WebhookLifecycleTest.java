package com.flightstats.hub.system.functional;

import com.flightstats.hub.system.extension.DependencyInjectionExtension;
import com.flightstats.hub.system.extension.HubLifecycleSuiteExtension;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.extension.DependencyInjectionResolver;
import com.flightstats.hub.system.extension.GuiceProviderExtension;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@ExtendWith(GuiceProviderExtension.class)
@ExtendWith(DependencyInjectionResolver.class)
@ExtendWith(HubLifecycleSuiteExtension.class)
@ExtendWith(DependencyInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebhookLifecycleTest {
    @Inject
    private CallbackService callbackResource;
    @Inject
    private ChannelService channelResource;
    @Inject
    private WebhookService webhookResource;
    @Inject
    private ModelBuilder modelBuilder;

    private String channelName;
    private String webhookName;

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

        channelResource.createWithDefaults(channelName);

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

        channelResource.createWithDefaults(channelName);
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

        channelResource.createWithDefaults(channelName);
        List<String> channelItems = channelResource.addItems(channelName, data, 10);
        Webhook webhook = buildWebhook().withStartItem(channelItems.get(4)).withParallelCalls(1);
        webhookResource.insertAndVerify(webhook);
        List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        assertTrue(callbackResource.areItemsEventuallySentToWebhook(webhookName, channelItemsExpected));
        assertEquals(channelItemsExpected, callbackResource.getItemsReceivedByCallback(webhookName));
    }

    @AfterEach
    @SneakyThrows
    void after() {
        channelResource.delete(channelName);
        webhookResource.delete(webhookName);
    }
}
