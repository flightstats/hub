package com.flightstats.hub.system.service;

import com.flightstats.hub.client.CallbackClientFactory;
import com.flightstats.hub.client.CallbackResourceClient;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.WebhookErrors;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    public HttpUrl getCallbackBaseUrl() {
        return callbackBaseUrl;
    }

    @SneakyThrows
    public boolean hasCallbackErrorInHub(String webhookName, String fullUrl) {
        String itemPath = Objects.requireNonNull(ContentKey.fromFullUrl(fullUrl)).toUrl();
        return Optional.ofNullable(webhookResource.getCallbackErrors(webhookName).body())
                .map(WebhookErrors::getErrors)
                .orElse(emptyList())
                .stream()
                .filter((error) -> webhookName.equals(error.getName()))
                .findFirst()
                .map(WebhookErrors.Error::getErrors)
                .orElse(emptyList())
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
        Call<String> call = callbackResourceClient.get(webhookName);
        final List<String> channelItemsPosted = new ArrayList<>();
        await().atMost(90, TimeUnit.SECONDS).until(() -> {
            Response<String> response = call.clone().execute();
            channelItemsPosted.clear();
            channelItemsPosted.addAll(
                    parseItemSentToWebhook(response.body())
                            .stream()
                            .filter((item) -> !path.isPresent() || item.contains(path.get()))
                            .collect(Collectors.toList()));

            return response.code() == OK.getStatusCode()
                    && channelItemsPosted.size() == expectedItemCount;
        });
        return channelItemsPosted;
    }
}
