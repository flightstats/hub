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
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class WebhookLifecycleTest extends BaseTest {

    private static final String EMPTY_STRING = "";

    private ChannelItemResourceClient channelItemResourceClient;
    private ChannelResourceClient channelResourceClient;
    private WebhookResourceClient webhookResourceClient;
    private CallbackResourceClient callbackResourceClient;
    private CallbackServer callbackServer;

    private String channelName;
    private String webhookName;

    private List<String> channelItemsPosted;

    @Before
    public void setup() {
        this.channelItemResourceClient = getHttpClient(ChannelItemResourceClient.class);
        this.channelResourceClient = getHttpClient(ChannelResourceClient.class);
        this.webhookResourceClient = getHttpClient(WebhookResourceClient.class);
        this.callbackResourceClient = getHttpClient(CallbackResourceClient.class);
        this.callbackServer = injector.getInstance(CallbackServer.class);

        this.channelName = generateRandomString();
        this.webhookName = generateRandomString();

        this.callbackServer.start();
    }

    @Test
    @SneakyThrows
    public void test() {
        final String data = "{\"first\": \"fn\", \"last\":\"ln\"}";

        createChannel();
        createWebhook();

        final List<String> channelItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            channelItems.add(addItemToChannel(data));
            log.info("Added channel item {} ", channelItems.get(i));
        }

        verifyWebhookCallback(channelItems);
    }

    @SneakyThrows
    private void verifyWebhookCallback(List<String> channelItems) {
        Call<String> call = callbackResourceClient.get(callbackServer.getUrl(), this.webhookName);
        await().atMost(Duration.FIVE_MINUTES).until(() -> {
            Response<String> response = call.clone().execute();
            channelItemsPosted = parseResponse(response.body());
            return response.code() == OK.getStatusCode()
                    && channelItemsPosted.size() == channelItems.size();
        });


        channelItems.replaceAll(String::trim);
        channelItemsPosted.replaceAll(String::trim);
        Collections.sort(channelItems);
        Collections.sort(channelItemsPosted);
        log.info("channelItemsPosted {} ", channelItemsPosted);
        log.info("channelItems {} ", channelItems);
        log.info("equals1 {} ", channelItemsPosted.equals(channelItems));
        log.info("equals2 {} ", channelItems.equals(channelItemsPosted));
        assertTrue(channelItems.equals(channelItemsPosted));
    }

    private List<String> parseResponse(String body) {
        if (!StringUtils.isBlank(body)) {
            String parsedString = body.replace("[", EMPTY_STRING).replace("]", EMPTY_STRING);
            return Arrays.asList(parsedString.split(","));
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
    private String addItemToChannel(Object data) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return response.body().get_links().getSelf().getHref();
    }

    @SneakyThrows
    private void createWebhook() {
        log.info("Webhook name {} ", webhookName);

        Call<com.flightstats.hub.model.Webhook> webhook = webhookResourceClient.create(webhookName, buildWebhook());
        Response<com.flightstats.hub.model.Webhook> response = webhook.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    private Webhook buildWebhook() {
        return Webhook.builder()
                .channelUrl(retrofit.baseUrl() + "channel/" + channelName)
                .callbackUrl(callbackServer.getUrl())
                .parallelCalls(2)
                .batch("single")
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
