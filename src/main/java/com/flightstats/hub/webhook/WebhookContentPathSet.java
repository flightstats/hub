package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentPath;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;

class WebhookContentPathSet {
    private final static Logger logger = LoggerFactory.getLogger(WebhookContentPathSet.class);
    private final static String BASE_PATH = "/GroupInFlight";

    private final CuratorFramework curator;

    @Inject
    public WebhookContentPathSet(CuratorFramework curator) {
        this.curator = curator;
    }

    public void add(String webhookName, ContentPath key) {
        String path = getPath(webhookName, key);
        try {
            curator.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException ignore) {
            logger.info("node exists " + path);
        } catch (Exception e) {
            logger.warn("unable to create " + path, e);
        }
    }

    public void remove(String webhookName, ContentPath key) {
        String path = getPath(webhookName, key);
        try {
            curator.delete().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete " + path, e);
        }
    }

    Set<ContentPath> getSet(String webhookName, ContentPath type) {
        String path = getPath(webhookName);
        Set<ContentPath> keys = new HashSet<>();
        try {
            List<String> strings = curator.getChildren().forPath(path);
            for (String string : strings) {
                keys.add(type.fromZk(string));
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node for {}", path);
        } catch (Exception e) {
            logger.warn("unable to get set " + path, e);
        }
        return keys;
    }

    Set<String> getWebhooks() {
        try {
            return new HashSet<>(curator.getChildren().forPath(BASE_PATH));
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node for {}", BASE_PATH);
            return newHashSet();
        } catch (Exception e) {
            logger.warn("unable to get webhooks for {}", BASE_PATH);
            return newHashSet();
        }
    }

    private String getPath(String webhookName) {
        return BASE_PATH + "/" + webhookName;
    }

    private String getPath(String webhookName, ContentPath key) {
        return getPath(webhookName) + "/" + key.toZk();
    }

    public void delete(String webhookName) {
        String path = getPath(webhookName);
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete {} {}", path, e.getMessage());
        }
    }
}
