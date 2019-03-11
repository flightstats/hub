package com.flightstats.hub.testcase;

import com.flightstats.hub.BaseTest;
import com.flightstats.hub.callback.CallbackServer;
import com.flightstats.hub.client.CallbackResourceClient;
import com.flightstats.hub.client.ChannelItemResourceClient;
import com.flightstats.hub.client.ChannelResourceClient;
import com.flightstats.hub.client.WebhookResourceClient;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.Webhook;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.model.ChannelContentStorageType.SINGLE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class WebhookLifecycleTest extends BaseTest {

    private static final String EMPTY_STRING = "";

    private ChannelItemResourceClient channelItemResourceClient;
    private ChannelResourceClient channelResourceClient;
    private WebhookResourceClient webhookResourceClient;
    private CallbackResourceClient callbackResourceClient;

    @Inject
    private CallbackServer callbackServer;

    private String channelName;
    private String webhookName;

    @Before
    public void setup() {
        super.setup();
        this.channelItemResourceClient = getHubClient(ChannelItemResourceClient.class);
        this.channelResourceClient = getHubClient(ChannelResourceClient.class);
        this.webhookResourceClient = getHubClient(WebhookResourceClient.class);

        this.callbackResourceClient = getCallbackClient(CallbackResourceClient.class);
        this.callbackServer.start();

        this.channelName = generateRandomString();
        this.webhookName = generateRandomString();
    }

    @Test
    @SneakyThrows
    public void testWebhookWithNoStartItem() {
        final String data = "{\"fn\": \"first\", \"ln\":\"last\"}";

        createChannel();

        final Webhook webhook = buildWebhook().withParallelCalls(2);
        addWebhook(webhook);

        final List<String> channelItems = addItemsToChannel(data, 10);
        final List<String> channelItemsPosted = getWebhookCallbackItems(channelItems.size());

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
        final List<String> channelItemsPosted = getWebhookCallbackItems(channelItemsExpected.size());

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
        final List<String> channelItemsPosted = getWebhookCallbackItems(channelItemsExpected.size());

        assertEquals(channelItemsExpected, channelItemsPosted);
    }

    @SneakyThrows
    private void logWebhookCallbackError() {
        log.info("Webhook name {} ", webhookName);

        Call<Object> call = webhookResourceClient.getError(webhookName);
        Response<Object> response = call.execute();

        assertEquals(OK.getStatusCode(), response.code());
        log.info("Call back errors for webhook {} {} ", webhookName, response.body());
    }


    private List<String> getWebhookCallbackItems(int expectedItemCount) {
        final List<String> channelItemsPosted = new ArrayList<>();
        Call<String> call = callbackResourceClient.get(webhookName);

        try {
            await().atMost(90, TimeUnit.SECONDS).until(() -> {
                Response<String> response = call.clone().execute();
                channelItemsPosted.clear();
                channelItemsPosted.addAll(parseResponse(response.body()));
                return response.code() == OK.getStatusCode()
                        && channelItemsPosted.size() == expectedItemCount;
            });
        } catch (Exception e) {
            log.error("Problem verifying webhook callbacks. {} ", e.getMessage());
            logWebhookCallbackError();
        }
        return channelItemsPosted;

    }

    private List<String> parseResponse(String body) {
        if (!isBlank(body)) {
            String parsedString = body.replace("[", EMPTY_STRING)
                    .replace("]", EMPTY_STRING);
            List<String> postedItems = Arrays.asList(parsedString.split(","));
            postedItems.replaceAll(String::trim);
            return postedItems;
        }
        return Collections.EMPTY_LIST;
    }

    @SneakyThrows
    private void createChannel() {
        log.info("Channel name {} ", channelName);

        Call<Object> call = channelResourceClient.create(Channel.builder().name(channelName).build());
        Response<Object> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    private void addWebhook(Webhook webhook) {
        log.info("Webhook name {} ", webhookName);

        Call<com.flightstats.hub.model.Webhook> call = webhookResourceClient.create(webhookName, webhook);
        Response<com.flightstats.hub.model.Webhook> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    private List<String> addItemsToChannel(Object data, int count) {
        final List<String> channelItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            channelItems.add(addItemToChannel(data));
        }
        return channelItems;
    }

    @SneakyThrows
    private String addItemToChannel(Object data) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return response.body().get_links().getSelf().getHref();
    }

    private Webhook buildWebhook() {
        return Webhook.builder()
                .channelUrl(getHubClientBaseUrl() + "channel/" + channelName)
                .callbackUrl(getCallbackClientBaseUrl() + "callback/")
                .batch(SINGLE.toString())
                .build();

    }

    @After
    @SneakyThrows
    public void cleanup() {
        this.channelResourceClient.delete(channelName).execute();
        this.webhookResourceClient.delete(webhookName).execute();

        this.callbackServer.stop();
    }

}
