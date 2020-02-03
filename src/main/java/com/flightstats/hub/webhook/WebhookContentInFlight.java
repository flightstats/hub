package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import javax.inject.Inject;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

class WebhookContentInFlight {
    private final static String BASE_PATH = "/GroupInFlight";

    private final SafeZooKeeperUtils zooKeeperUtils;

    @Inject
    public WebhookContentInFlight(SafeZooKeeperUtils zooKeeperUtils) {
        this.zooKeeperUtils = zooKeeperUtils;
    }

    public void add(String webhookName, ContentPath key) {
        zooKeeperUtils.createPathAndParents(BASE_PATH, webhookName, key.toZk());
    }

    public void remove(String webhookName, ContentPath key) {
        zooKeeperUtils.delete(BASE_PATH, webhookName, key.toZk());
    }

    public Set<ContentPath> getSet(String webhookName, ContentPath type) {
        return zooKeeperUtils.getChildren(BASE_PATH, webhookName)
                .stream()
                .map(type::fromZk)
                .collect(toSet());
    }

    public void delete(String webhookName) {
        zooKeeperUtils.deletePathAndChildren(BASE_PATH, webhookName);
    }
}
