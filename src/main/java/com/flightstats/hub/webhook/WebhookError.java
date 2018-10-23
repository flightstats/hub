package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.RequestUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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

import static com.flightstats.hub.util.RequestUtils.getChannelName;

/**
 * This class is responsible for creation and management of webhook error strings.
 *
 * Format: "{timestamp} {contentKey} {message}"
 *
 * Example: "2018-01-02T03:04:05.006Z 2018/01/02/03/04/05/006/abcdef 400 Bad Request"
 *  - timestamp: 2018-01-02T03:04:05.006Z
 *  - contentKey: 2018/01/02/03/04/05/006/abcdef
 *  - message: 400 Bad Request
 */

@Singleton
class WebhookError {
    private final static Logger logger = LoggerFactory.getLogger(WebhookError.class);
    private static final int MAX_SIZE = 10;

    private final CuratorFramework curator;
    private final ChannelService channelService;
    private final HubProperties hubProperties;

    @Inject
    public WebhookError(CuratorFramework curator, ChannelService channelService, HubProperties hubProperties) {
        this.curator = curator;
        this.channelService = channelService;
        this.hubProperties = hubProperties;
    }

    public void add(String webhook, String error) {
        String path = getErrorRoot(webhook) + "/" + TimeUtil.now().getMillis() + StringUtils.randomAlphaNumeric(6);
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
        } catch (KeeperException.NoNodeException ignore) {
            logger.debug(ignore.getMessage());
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

    void publishToErrorChannel(DeliveryAttempt attempt) {
        if (attempt.getWebhook().getErrorChannelUrl() == null) return;

        List<String> errors = get(attempt.getWebhook().getName());
        if (errors.size() < 1) {
            logger.debug("no errors found for", attempt.getWebhook().getName());
            return;
        }

        String error = errors.get(errors.size() - 1);
        byte[] bytes = buildPayload(attempt, error);
        long contentLength = (long) bytes.length;
        Content content = Content.builder()
                .withContentType("application/json")
                .withContentLength(contentLength)
                .withLarge(contentLength >= hubProperties.getLargePayload())
                .withData(bytes)
                .build();

        try {
            channelService.insert(getChannelName(attempt.getWebhook().getErrorChannelUrl()), content);
        } catch (Exception e) {
            logger.warn("unable to publish errors for " + attempt.getWebhook().getName(), e);
        }
    }

    private byte[] buildPayload(DeliveryAttempt attempt, String error) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("webhookUrl", buildWebhookUrl(attempt));
        root.put("failedItemUrl", attempt.getWebhook().getChannelUrl() + "/" + attempt.getContentPath().toUrl());
        root.put("callbackUrl", attempt.getWebhook().getCallbackUrl());
        root.put("numberOfAttempts", attempt.getNumber() - 1);
        root.put("lastAttemptTime", extractTimestamp(error));
        root.put("lastAttemptError", extractMessage(error));
        return root.toString().getBytes();
    }

    private String buildWebhookUrl(DeliveryAttempt attempt) {
        // todo - workaround for HubHost.getLocalNamePort and HubProperties.getAppUrl not returning usable URLs for dockerized single hub
        String host = RequestUtils.getHost(attempt.getWebhook().getChannelUrl());
        return host + "/webhook/" + attempt.getWebhook().getName();
    }

    private String extractTimestamp(String error) {
        return error.substring(0, error.indexOf(" "));
    }

    private String extractMessage(String error) {
        int firstSpace = error.indexOf(" ");
        int secondSpace = error.indexOf(" ", firstSpace + 1);
        return error.substring(secondSpace + 1);
    }

    private static class Error {
        String name;
        DateTime creationTime;
        String data;

        @java.beans.ConstructorProperties({"name", "creationTime", "data"})
        Error(String name, DateTime creationTime, String data) {
            this.name = name;
            this.creationTime = creationTime;
            this.data = data;
        }

        public static ErrorBuilder builder() {
            return new ErrorBuilder();
        }

        public String getName() {
            return this.name;
        }

        public DateTime getCreationTime() {
            return this.creationTime;
        }

        public String getData() {
            return this.data;
        }

        public static class ErrorBuilder {
            private String name;
            private DateTime creationTime;
            private String data;

            ErrorBuilder() {
            }

            public Error.ErrorBuilder name(String name) {
                this.name = name;
                return this;
            }

            public Error.ErrorBuilder creationTime(DateTime creationTime) {
                this.creationTime = creationTime;
                return this;
            }

            public Error.ErrorBuilder data(String data) {
                this.data = data;
                return this;
            }


            public Error build() {
                return new Error(name, creationTime, data);
            }

            public String toString() {
                return "com.flightstats.hub.webhook.WebhookError.Error.ErrorBuilder(name=" + this.name + ", creationTime=" + this.creationTime + ", data=" + this.data + ")";
            }
        }
    }
}
