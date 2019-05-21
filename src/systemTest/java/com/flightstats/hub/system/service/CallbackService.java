package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.callback.CallbackClientFactory;
import com.flightstats.hub.clients.callback.CallbackResourceClient;
import com.flightstats.hub.model.WebhookCallback;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.awaitility.Awaitility.await;

@Slf4j
public class CallbackService {

    private static final String EMPTY_STRING = "";
    private CallbackResourceClient callbackResourceClient;
    private WebhookService webhookResource;
    private HttpUrl callbackBaseUrl;

    @Inject
    public CallbackService(CallbackClientFactory callbackClientFactory, WebhookService webhookResource) {
        this.callbackResourceClient = callbackClientFactory.getCallbackClient(CallbackResourceClient.class);
        this.webhookResource = webhookResource;
        this.callbackBaseUrl = callbackClientFactory.getCallbackUrl();
    }

    public String getCallbackUrl(String webhookName) {
        return getCallbackBaseUrl() + "callback/" + webhookName;
    }

    private HttpUrl getCallbackBaseUrl() {
        return callbackBaseUrl;
    }

    @SneakyThrows
    public boolean hasCallbackErrorInHub(String webhookName, String fullUrl) {
        String itemPath = ContentKey.fromFullUrl(fullUrl).toUrl();
        return webhookResource.getCallbackErrors(webhookName).body().getErrors()
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

    private List<String> parseItemSentToWebhook(String body) {
        if (!isBlank(body)) {
            String parsedString = body.replace("[", EMPTY_STRING)
                    .replace("]", EMPTY_STRING);
            List<String> postedItems = Arrays.asList(parsedString.split(","));
            postedItems.replaceAll(String::trim);
            return postedItems;
        }
        return emptyList();
    }


    public List<String> awaitItemCountSentToWebhook(String webhookName, Optional<String> path, int expectedItemCount) {
        Call<WebhookCallback> call = callbackResourceClient.get(webhookName);
        List<String> channelItemsPosted = new ArrayList<>();
        await().atMost(90, TimeUnit.SECONDS).until(() -> {
            Response<WebhookCallback> response = call.clone().execute();
            channelItemsPosted.clear();
            channelItemsPosted.addAll(Optional.ofNullable(response.body())
                    .map(WebhookCallback::getUris)
                    .orElse(emptyList()));

            return response.code() == OK.getStatusCode()
                    && channelItemsPosted.size() == expectedItemCount;
        });
        return channelItemsPosted;
    }

}
