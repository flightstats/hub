package com.flightstats.hub.testcase;

import com.flightstats.hub.model.Webhook;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Slf4j
public class WebhookLifecycleTest extends WebhookTest {

    @Before
    public void setup() {
        super.setup();
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Test
    @SneakyThrows
    public void testWebhookWithNoStartItem() {
        final String data = "{\"fn\": \"first\", \"ln\":\"last\"}";

        createChannel();

        final Webhook webhook = buildWebhook().withParallelCalls(2);
        addWebhook(webhook);

        final List<String> channelItems = addItemsToChannel(data, 10);
        final List<String> channelItemsPosted = awaitItemCountSentToWebhook(channelItems.size());

        Collections.sort(channelItems);
        Collections.sort(channelItemsPosted);
        assertEquals(channelItems, channelItemsPosted);
    }

    @Test
    @SneakyThrows
    public void testWebhookWithStartItem() {
        final String data = "{\"key1\": \"value1\", \"key2\":\"value2\"}";

        createChannel();
        final List<String> channelItems = addItemsToChannel(data, 10);

        final Webhook webhook = buildWebhook().
                withStartItem(channelItems.get(4)).
                withParallelCalls(2);
        addWebhook(webhook);
        final List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        final List<String> channelItemsPosted = awaitItemCountSentToWebhook(channelItemsExpected.size());

        Collections.sort(channelItemsExpected);
        Collections.sort(channelItemsPosted);
        assertEquals(channelItemsExpected, channelItemsPosted);
    }

    @Test
    @SneakyThrows
    public void testWebhookWithStartItem_expectItemsInOrder() {
        final String data = "{\"city\": \"portland\", \"state\":\"or\"}";

        createChannel();
        final List<String> channelItems = addItemsToChannel(data, 10);

        final Webhook webhook = buildWebhook().
                withStartItem(channelItems.get(4)).
                withParallelCalls(1);
        addWebhook(webhook);
        final List<String> channelItemsExpected = channelItems.subList(5, channelItems.size());
        final List<String> channelItemsPosted = awaitItemCountSentToWebhook(channelItemsExpected.size());

        assertEquals(channelItemsExpected, channelItemsPosted);
    }


}
