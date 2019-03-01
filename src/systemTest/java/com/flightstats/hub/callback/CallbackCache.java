package com.flightstats.hub.callback;

import com.flightstats.hub.callback.model.WebhookCallbackRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CallbackCache {

    private final Map<String, List<String>> cacheRequestObject = new ConcurrentHashMap<>();

    public void put(WebhookCallbackRequest webhookCallbackRequest) {
        String webhookName = webhookCallbackRequest.getName();

        synchronized (this) {
            if (cacheRequestObject.containsKey(webhookName)) {
                cacheRequestObject.get(webhookName).addAll(webhookCallbackRequest.getUris());
            } else {
                cacheRequestObject.put(
                        webhookCallbackRequest.getName(),
                        new ArrayList<>(webhookCallbackRequest.getUris()));
            }
        }
    }

    public List<String> get(String webhookName) {
        return cacheRequestObject.get(webhookName);
    }

    public boolean contains(String webhookName) {
        return cacheRequestObject.containsKey(webhookName);
    }
}
