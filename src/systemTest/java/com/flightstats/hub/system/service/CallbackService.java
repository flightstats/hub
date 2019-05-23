package com.flightstats.hub.system.service;

import com.flightstats.hub.client.CallbackClientFactory;
import com.flightstats.hub.client.CallbackResourceClient;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.WebhookErrors;
import com.flightstats.hub.model.WebhookCallback;
import com.flightstats.hub.model.WebhookErrors;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    public HttpUrl getCallbackBaseUrl() {
        return callbackBaseUrl;
    }

    @SneakyThrows
    public boolean hasCallbackErrorInHub(String webhookName, String fullUrl) {
        String itemPath = ContentKey.fromFullUrl(fullUrl).toUrl();
        return getCallbackErrorsInHub(webhookName)
                .stream()
                .anyMatch((path) -> path.contains(itemPath));
    }

    public List<String> getCallbackErrorsInHub(String webhookName) {
        List<WebhookErrors.Error> errors = Optional.ofNullable(webhookResource.getCallbackErrors(webhookName).body())
                .map(WebhookErrors::getErrors)
                .orElse(emptyList());
        return errors.stream()
                .filter((error) -> webhookName.equals(error.getName()))
                .findFirst()
                .map(WebhookErrors.Error::getErrors)
                .orElse(emptyList());
    }

    public boolean awaitHubHasCallbackErrorForItemPath(String webhookName, String path) {
        try {
            await().atMost(90, TimeUnit.SECONDS).until(() -> hasCallbackErrorInHub(webhookName, path));
            return true;
        } catch (Exception e) {
            log.error("Unable to see callback error on hub for {} {} due to {}", webhookName, path, e.getMessage());
            return false;
        }
    }

    public boolean isErrorListEventuallyCleared(String webhookName) {
        try {
            await().atMost(90, TimeUnit.SECONDS).until(() -> getCallbackErrorsInHub(webhookName).isEmpty());
            return true;
        } catch (Exception e) {
            log.error("Unable to see callback error on hub for {} due to {}", webhookName, e.getMessage());
            return false;
        }
    }

    public List<String> awaitItemCountSentToWebhook(String webhookName, int expectedItemCount) {
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
