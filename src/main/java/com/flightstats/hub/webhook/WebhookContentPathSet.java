package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.google.inject.Inject;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

class WebhookContentPathSet {
    private final static String BASE_PATH = "/GroupInFlight";

    private final SafeZooKeeperUtils zooKeeperUtils;

    @Inject
    public WebhookContentPathSet(SafeZooKeeperUtils zooKeeperUtils) {
        this.zooKeeperUtils = zooKeeperUtils;
    }

    public void add(String webhookName, ContentPath key) {
        zooKeeperUtils.createPathAndParents(BASE_PATH, webhookName, key.toZk());
    }

    public void remove(String webhookName, ContentPath key) {
        zooKeeperUtils.deletePathAndChildren(BASE_PATH, webhookName, key.toZk());
    }

    public void delete(String webhookName) {
        zooKeeperUtils.deletePathAndChildren(BASE_PATH, webhookName);
    }

    Set<ContentPath> getSet(String webhookName, ContentPath type) {
        return zooKeeperUtils.getChildren(BASE_PATH, webhookName)
                .stream()
                .map(type::fromZk)
                .collect(toSet());
    }

    Set<String> getWebhooks() {
        return new HashSet<>(zooKeeperUtils.getChildren(BASE_PATH));
    }
}
