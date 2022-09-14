package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.webhook.WebhookResourceClient;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookErrors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class WebhookService {

    private final WebhookResourceClient webhookResourceClient;

    @Inject
    public WebhookService(HubClientFactory hubClientFactory) {
        this.webhookResourceClient = hubClientFactory.getHubClient(WebhookResourceClient.class);
    }

    @SneakyThrows
    public void insertAndVerify(Webhook webhook) {
        assertEquals(CREATED.getStatusCode(), upsert(webhook));
    }

    @SneakyThrows
    public void updateAndVerify(Webhook webhook) {
        assertEquals(OK.getStatusCode(), upsert(webhook));
    }

    @SneakyThrows
    private int upsert(Webhook webhook) {
        log.debug("Upsert webhook name {} ", webhook.getName());

        Response<Webhook> response = webhookResourceClient.create(webhook.getName(), webhook).execute();
        return response.code();
    }

    @SneakyThrows
    Response<WebhookErrors> getCallbackErrors(String webhookName) {
        return webhookResourceClient.getError(webhookName).execute();
    }

    public String getChannelName(Webhook webhook) {
        try {
            List<String> channelPath = Arrays.asList(webhook.getChannelUrl().split("/"));
            return channelPath.get(channelPath.indexOf("channel") + 1);
        } catch (Exception e) {
            log.error("failed to find channel name in webhook config");
            return "";
        }
    }

    @SneakyThrows
    public Webhook get(String webhookName) {
        try {
            Call<Webhook> call = webhookResourceClient.get(webhookName);
            Response<Webhook> response = call.execute();
            return response.body();
        } catch(Exception e) {
            log.error("failed to get webhook: {} {}", webhookName, e.getMessage());
            throw e;
        }
    }

    @SneakyThrows
    public void delete(String webhookName) {
        webhookResourceClient.delete(webhookName).execute();
    }

}
