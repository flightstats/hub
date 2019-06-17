package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.callback.CallbackClientFactory;
import com.flightstats.hub.clients.callback.CallbackResourceClient;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.WebhookErrors;
import com.flightstats.hub.model.WebhookCallback;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;

@Slf4j
public class CallbackService {

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
            log.error("Callback error on hub was never cleared for {} due to {}", webhookName, e.getMessage());
            return false;
        }
    }

    public boolean areItemsEventuallySentToWebhook(String webhookName, List<String> expectedChannelItems) {
        try {
            await().atMost(90, TimeUnit.SECONDS).until(() ->
                getItemsReceivedByCallback(webhookName).containsAll(expectedChannelItems));
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public List<String> getItemsReceivedByCallback(String webhookName) {
        return getReceivedItemsResponse(webhookName)
                .filter(response -> response.code() == OK.getStatusCode())
                .flatMap(response -> Optional.ofNullable(response.body()))
                .map(WebhookCallback::getUris)
                .orElse(emptyList());
    }

    private Optional<Response<WebhookCallback>> getReceivedItemsResponse(String webhookName) {
        try {
            Call<WebhookCallback> call = callbackResourceClient.get(webhookName);
            return Optional.of(call.clone().execute());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }
}
