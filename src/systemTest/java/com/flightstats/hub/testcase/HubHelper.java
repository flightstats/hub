package com.flightstats.hub.testcase;

import com.flightstats.hub.client.ChannelItemResourceClient;
import com.flightstats.hub.client.ChannelResourceClient;
import com.flightstats.hub.client.WebhookResourceClient;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookErrors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class HubHelper {
    private Retrofit retrofitHub;
    private ChannelItemResourceClient channelItemResourceClient;
    private ChannelResourceClient channelResourceClient;
    private WebhookResourceClient webhookResourceClient;

    @Inject
    public HubHelper(@Named("hub") Retrofit retrofitHub) {
        this.retrofitHub = retrofitHub;
        this.channelItemResourceClient = getHubClient(ChannelItemResourceClient.class);
        this.channelResourceClient = getHubClient(ChannelResourceClient.class);
        this.webhookResourceClient = getHubClient(WebhookResourceClient.class);
    }

    @SneakyThrows
    protected void logCurrentCallbackErrorsFromHub(String webhookName) {
        log.info("Webhook name {} ", webhookName);
        Response<WebhookErrors> response = getCallbackErrorsForCurrentWebhook(webhookName);
        verifyErrorsFor(response, webhookName);
        log.info("Call back errors for webhook {} {} ", webhookName, response.body());
    }

    private void verifyErrorsFor(Response<WebhookErrors> response, String webhookName) {
        WebhookErrors body = response.body();
        assertEquals(OK.getStatusCode(), response.code());
        assertFalse(body.getErrors().isEmpty());
        assertEquals(webhookName, body.getErrors().get(0).getName());
    }

    private <T> T getHubClient(Class<T> serviceClass) {
        return retrofitHub.create(serviceClass);
    }

    @SneakyThrows
    private Response<WebhookErrors> getCallbackErrorsForCurrentWebhook(String webhookName) {
        return webhookResourceClient.getError(webhookName).execute();
    }

    @SneakyThrows
    private int upsertWebhook(Webhook webhook) {
        log.info("Upsert webhook name {} ", webhook.getName());

        Call<Webhook> call = webhookResourceClient.create(webhook.getName(), webhook);
        Response<Webhook> response = call.execute();

        return response.code();
    }

    public HttpUrl getHubClientBaseUrl() {
        return retrofitHub.baseUrl();
    }

    @SneakyThrows
    public boolean hasCallbackErrorInHub(String webhookName, String fullUrl) {
        String itemPath = ContentKey.fromFullUrl(fullUrl).toUrl();
        return getCallbackErrorsForCurrentWebhook(webhookName).body().getErrors()
                .stream()
                .filter((error) -> webhookName.equals(error.getName()))
                .findFirst().get().getErrors()
                .stream()
                .anyMatch((path) -> path.contains(itemPath));
    }

    public void awaitHubHasCallbackErrorForItemPath(String webhookName, String path) {
        try {
            await().atMost(90, TimeUnit.SECONDS).until(() -> hasCallbackErrorInHub(webhookName, path));
        } catch (Exception e) {
            log.error("Unable to see callback error on hub for {} {} due to {}", webhookName, path, e.getMessage());
        }
    }

    @SneakyThrows
    public void createChannel(String channelName) {
        log.info("Create channel name {} ", channelName);

        Call<Object> call = channelResourceClient.create(Channel.builder().name(channelName).build());
        Response<Object> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    public void insertAndVerifyWebhook(Webhook webhook) {
        assertEquals(CREATED.getStatusCode(), upsertWebhook(webhook));
    }

    @SneakyThrows
    public void updateAndVerifyWebhook(Webhook webhook) {
        assertEquals(OK.getStatusCode(), upsertWebhook(webhook));
    }

    public List<String> addItemsToChannel(String channelName, Object data, int count) {
        final List<String> channelItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            channelItems.add(addItemToChannel(channelName, data));
        }
        return channelItems;
    }

    @SneakyThrows
    public String addItemToChannel(String channelName, Object data) {
        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
        assertNotNull(response);
        assertNotNull(response.body());

        return response.body().get_links().getSelf().getHref();
    }

    @SneakyThrows
    public void deleteChannelAndWebhook(String channelName, String webhookName) {
        deleteWebhook(webhookName);
        this.channelResourceClient.delete(channelName).execute();
    }

    @SneakyThrows
    public void deleteWebhook(String webhookName) {
        webhookResourceClient.delete(webhookName).execute();
    }
}