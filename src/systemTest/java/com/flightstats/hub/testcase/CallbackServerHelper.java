package com.flightstats.hub.testcase;

import com.flightstats.hub.callback.CallbackResource;
import com.flightstats.hub.callback.CallbackServer;
import com.flightstats.hub.client.CallbackResourceClient;
import com.flightstats.hub.model.WebhookCallbackRequest;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.awaitility.Awaitility.await;

@Slf4j
public class CallbackServerHelper {
    private static final String EMPTY_STRING = "";
    private Retrofit retrofitCallback;
    private CallbackResourceClient callbackResourceClient;
    private CallbackServer callbackServer;
    private CallbackResource callbackResource;

    @Inject
    CallbackServerHelper(@Named("callback") Retrofit retrofitCallback, CallbackServer callbackServer, CallbackResource callbackResource){
        this.retrofitCallback = retrofitCallback;
        this.callbackResourceClient = getCallbackClient(CallbackResourceClient.class);
        this.callbackServer = callbackServer;
        this.callbackResource = callbackResource;
    }

    private <T> T getCallbackClient(Class<T> serviceClass) {
        return retrofitCallback.create(serviceClass);
    }

    private List<String> parseItemSentToWebhook(String body) {
        if (!isBlank(body)) {
            String parsedString = body.replace("[", EMPTY_STRING)
                    .replace("]", EMPTY_STRING);
            List<String> postedItems = Arrays.asList(parsedString.split(","));
            postedItems.replaceAll(String::trim);
            return postedItems;
        }
        return Collections.emptyList();
    }

    void startCallbackServer() {
        this.callbackServer.start();
    }

    void stopCallbackServer() {
        callbackServer.stop();
    }

    HttpUrl getCallbackClientBaseUrl() {
        return retrofitCallback.baseUrl();
    }

    List<String> awaitItemCountSentToWebhook(String webhookName, Optional<String> path, int expectedItemCount) {
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

    void errorOnCreate(Predicate<WebhookCallbackRequest> predicate) {
        callbackResource.errorOnCreate(predicate);
    }

}