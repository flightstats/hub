package com.flightstats.hub.testcase;

import com.flightstats.hub.BaseTest;
import com.flightstats.hub.model.Webhook;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.flightstats.hub.model.ChannelContentStorageType.SINGLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class WebhookLifecycleTest extends BaseTest {
    @Inject
    private HubHelper hubHelper;
    @Inject
    private CallbackServerHelper callbackServerHelper;
    private String channelName;
    private String webhookName;

    @BeforeEach
    public void before() {
        super.before();
        callbackServerHelper.startCallbackServer();

        this.channelName = generateRandomString();
        this.webhookName = generateRandomString();
    }

    private Webhook buildWebhook() {
        return Webhook.builder()
                .name(webhookName)
                .channelUrl(hubHelper.getHubClientBaseUrl() + "channel/" + channelName)
                .callbackUrl(callbackServerHelper.getCallbackClientBaseUrl() + "callback/")
                .batch(SINGLE.toString())
                .build();
    }

    @Test
    @SneakyThrows
    void testWebhookWithNoStartItem() {
        final String data = "{\"fn\": \"first\", \"ln\":\"last\"}";

        hubHelper.createChannel(channelName);

        final Webhook webhook = buildWebhook().withParallelCalls(2);
        hubHelper.insertAndVerifyWebhook(webhook);

        final List<String> channelItems = hubHelper.addItemsToChannel(channelName, data, 10);
        final List<String> channelItemsPosted = callbackServerHelper.awaitItemCountSentToWebhook(webhookName, Optional.empty(), channelItems.size());

        Collections.sort(channelItems);
        Collections.sort(channelItemsPosted);
        assertEquals(channelItems, channelItemsPosted);
    }

    @Test
    @SneakyThrows
    void testWebhookWithStartItem() {
        final String data = "{\"key1\": \"value1\", \"key2\":\"value2\"}";

        hubHelper.createChannel(channelName);
        final List<String> channelItems = hubHelper.addItemsToChannel(channelName, data, 10);

        final Webhook webhook = buildWebhook().
                withStartItem(channelItems.get(4)).
                withParallelCalls(2);
        hubHelper.insertAndVerifyWebhook(webhook);
        final List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        final List<String> channelItemsPosted = callbackServerHelper.awaitItemCountSentToWebhook(webhookName, Optional.empty(), channelItemsExpected.size());

        Collections.sort(channelItemsExpected);
        Collections.sort(channelItemsPosted);
        assertEquals(channelItemsExpected, channelItemsPosted);
    }

    @Test
    @SneakyThrows
    void testWebhookWithStartItem_expectItemsInOrder() {
        final String data = "{\"city\": \"portland\", \"state\":\"or\"}";

        hubHelper.createChannel(channelName);
        final List<String> channelItems = hubHelper.addItemsToChannel(channelName, data, 10);

        final Webhook webhook = buildWebhook().
                withStartItem(channelItems.get(4)).
                withParallelCalls(1);
        hubHelper.insertAndVerifyWebhook(webhook);
        final List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        final List<String> channelItemsPosted = callbackServerHelper.awaitItemCountSentToWebhook(webhookName, Optional.empty(), channelItemsExpected.size());

        assertEquals(channelItemsExpected, channelItemsPosted);
    }

    @AfterEach
    @SneakyThrows
    void after() {
        hubHelper.deleteChannelAndWebhook(channelName, webhookName);
        callbackServerHelper.stopCallbackServer();
    }

}
