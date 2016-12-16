package com.flightstats.hub.webhook;

import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Singleton
class WebhookError {
    private final static Logger logger = LoggerFactory.getLogger(WebhookError.class);
    private static final int MAX_SIZE = 10;

    private final CuratorFramework curator;

    @Inject
    public WebhookError(CuratorFramework curator) {
        this.curator = curator;
    }

    public void add(String webhook, String error) {
        String path = getErrorRoot(webhook) + "/" + TimeUtil.now().getMillis() + RandomStringUtils.randomAlphanumeric(6);
        try {
            curator.create().creatingParentsIfNeeded().forPath(path, error.getBytes());
        } catch (Exception e) {
            logger.warn("unable to create " + path, e);
        }
        limitChildren(webhook);
    }

    private List<String> limitChildren(String webhook) {
        String errorRoot = getErrorRoot(webhook);
        List<String> results = new ArrayList<>();
        SortedMap<String, Error> errors = new TreeMap<>();
        try {
            for (String child : curator.getChildren().forPath(errorRoot)) {
                Stat stat = new Stat();
                byte[] bytes = curator.getData().storingStatIn(stat).forPath(getChildPath(errorRoot, child));
                errors.put(child, new Error(child, new DateTime(stat.getCtime()), new String(bytes)));
            }
            while (errors.size() > MAX_SIZE) {
                String firstKey = errors.firstKey();
                errors.remove(firstKey);
                curator.delete().inBackground().forPath(getChildPath(errorRoot, firstKey));
            }
            DateTime cutoffTime = TimeUtil.now().minusDays(1);
            for (Error error : errors.values()) {
                if (error.getCreationTime().isBefore(cutoffTime)) {
                    curator.delete().inBackground().forPath(getChildPath(errorRoot, error.getName()));
                } else {
                    results.add(error.getData());
                }
            }
        } catch (Exception e) {
            logger.warn("unable to limit children " + errorRoot, e);
        }
        return results;
    }

    public void delete(String webhook) {
        String errorRoot = getErrorRoot(webhook);
        logger.info("deleting " + errorRoot);
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(errorRoot);
        } catch (KeeperException.NoNodeException e) {
            logger.info("unable to delete missing node " + errorRoot);
        } catch (Exception e) {
            logger.warn("unable to delete " + errorRoot, e);
        }
    }

    private String getErrorRoot(String webhook) {
        return "/GroupError/" + webhook;
    }

    private String getChildPath(String errorRoot, String child) {
        return errorRoot + "/" + child;
    }

    public List<String> get(String webhook) {
        return limitChildren(webhook);
    }

    @Builder
    @Getter
    private static class Error {
        String name;
        DateTime creationTime;
        String data;
    }
}
