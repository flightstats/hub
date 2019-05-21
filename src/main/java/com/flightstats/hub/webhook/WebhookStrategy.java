package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.ClusterStateDao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.util.TimeUtil;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

interface WebhookStrategy extends AutoCloseable {

    static ContentPath createContentPath(Webhook webhook) {
        if (webhook.isSecond()) {
            return new SecondPath();
        }
        if (webhook.isMinute()) {
            return new MinutePath();
        }
        return new ContentKey(TimeUtil.now(), "initial");
    }

    static WebhookStrategy getStrategy(ContentRetriever contentRetriever,
                                       ClusterStateDao clusterStateDao,
                                       ObjectMapper objectMapper,
                                       Webhook webhook) {
        if (webhook.isMinute() || webhook.isSecond()) {
            return new TimedWebhookStrategy(contentRetriever, clusterStateDao, objectMapper, webhook);
        }
        return new SingleWebhookStrategy(contentRetriever, clusterStateDao, objectMapper, webhook);
    }

    static void close(AtomicBoolean shouldExit, ExecutorService executorService, BlockingQueue queue) {
        if (!shouldExit.get()) {
            shouldExit.set(true);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (queue != null) {
            queue.clear();
        }
    }

    ContentPath getStartingPath();

    ContentPath getLastCompleted();

    void start(Webhook webhook, ContentPath startingKey);

    Optional<ContentPath> next() throws Exception;

    ObjectNode createResponse(ContentPath contentPath);

    ContentPath inProcess(ContentPath contentPath);
}
