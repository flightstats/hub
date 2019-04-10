package com.flightstats.hub.testcase;

import com.flightstats.hub.BaseTest;
import com.flightstats.hub.callback.CallbackResource;
import com.flightstats.hub.callback.CallbackServer;
import com.flightstats.hub.client.CallbackResourceClient;
import com.flightstats.hub.client.ChannelItemResourceClient;
import com.flightstats.hub.client.ChannelResourceClient;
import com.flightstats.hub.client.WebhookResourceClient;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookErrors;
import lombok.SneakyThrows;
import org.junit.After;
import org.slf4j.Logger;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public abstract class WebhookTest extends BaseTest {
    private static final String EMPTY_STRING = "";
    private Logger log;
    protected ChannelItemResourceClient channelItemResourceClient;
    protected ChannelResourceClient channelResourceClient;
    protected WebhookResourceClient webhookResourceClient;
    protected CallbackResourceClient callbackResourceClient;
    @Inject
    protected CallbackServer callbackServer;
    @Inject
    protected CallbackResource callbackResource;
    protected String channelName;
    protected String webhookName;

    public void setup() {
        super.setup();
        this.log = getLog();
        this.channelItemResourceClient = getHubClient(ChannelItemResourceClient.class);
        this.channelResourceClient = getHubClient(ChannelResourceClient.class);
        this.webhookResourceClient = getHubClient(WebhookResourceClient.class);

        this.callbackResourceClient = getCallbackClient(CallbackResourceClient.class);
        this.callbackServer.start(super.injector);

        this.channelName = generateRandomString();
        this.webhookName = generateRandomString();
    }

    protected abstract Logger getLog();

    @SneakyThrows
    protected void logWebhookCallbackError() {
        log.info("Webhook name {} ", webhookName);
        WebhookErrors body = getWebhookCallbackError().body();
        log.info("Call back errors for webhook {} {} ", webhookName, body);
    }

    @SneakyThrows
    protected Response<WebhookErrors> getWebhookCallbackError() {
        Call<WebhookErrors> call = webhookResourceClient.getError(webhookName);
        Response<WebhookErrors> response = call.execute();

        WebhookErrors body = response.body();
        assertEquals(OK.getStatusCode(), response.code());
        assertFalse(body.getErrors().isEmpty());
        assertEquals(webhookName, body.getErrors().get(0).getName());

        return response;
    }

    @SneakyThrows
    protected boolean hasCallbackErrorForFullUrl(String webhookName, String fullUrl) {
        String itemPath = ContentKey.fromFullUrl(fullUrl).toUrl();
        return getWebhookCallbackError().body().getErrors()
                .stream()
                .filter((error) -> webhookName.equals(error.getName()))
                .findFirst().get().getErrors()
                    .stream()
                    .anyMatch((path) -> path.contains(itemPath));
    }

    protected List<String> getWebhookCallbackItems(int expectedItemCount) {
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

    protected List<String> parseResponse(String body) {
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
    protected void createChannel() {
        log.info("Channel name {} ", channelName);

        Call<Object> call = channelResourceClient.create(Channel.builder().name(channelName).build());
        Response<Object> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    protected void addWebhook(Webhook webhook) {
        log.info("Webhook name {} ", webhookName);

        Call<Webhook> call = webhookResourceClient.create(webhookName, webhook);
        Response<Webhook> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    protected List<String> addItemsToChannel(Object data, int count) {
        final List<String> channelItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            channelItems.add(addItemToChannel(data));
        }
        return channelItems;
    }

    @SneakyThrows
    protected String addItemToChannel(Object data) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return response.body().get_links().getSelf().getHref();
    }

    protected Webhook buildWebhook() {
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
