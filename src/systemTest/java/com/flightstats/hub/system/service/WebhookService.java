package com.flightstats.hub.system.service;

import com.flightstats.hub.client.HubClientFactory;
import com.flightstats.hub.client.WebhookResourceClient;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookErrors;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class WebhookService {

    private WebhookResourceClient webhookResourceClient;

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
        log.info("Upsert webhook name {} ", webhook.getName());

        Call<Webhook> call = webhookResourceClient.create(webhook.getName(), webhook);
        Response<Webhook> response = call.execute();
        log.info("webhook creation response {} ", response.body());
        return response.code();
    }

    @SneakyThrows
    public Response<WebhookErrors> getCallbackErrors(String webhookName) {
        return webhookResourceClient.getError(webhookName).execute();
    }

    @SneakyThrows
    public void delete(String webhookName) {
        webhookResourceClient.delete(webhookName).execute();
    }

}
