package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;

@Slf4j
class SingleWebhookStrategy implements WebhookStrategy {
    private AtomicReference<Exception> exceptionReference = new AtomicReference<>();
    private AtomicBoolean shouldExit = new AtomicBoolean(false);

    private final ClusterCacheDao clusterCacheDao;
    private final ContentRetriever contentRetriever;
    private final ObjectMapper objectMapper;
    private final Webhook webhook;

    private BlockingQueue<ContentPath> queue;
    private QueryGenerator queryGenerator;
    private ExecutorService executorService;
    private String channel;

    @Inject
    SingleWebhookStrategy(ContentRetriever contentRetriever,
                          ClusterCacheDao clusterCacheDao,
                          ObjectMapper objectMapper,
                          Webhook webhook) {
        this.contentRetriever = contentRetriever;
        this.clusterCacheDao = clusterCacheDao;
        this.objectMapper = objectMapper;
        this.webhook = webhook;
        this.queue = new ArrayBlockingQueue<>(webhook.getParallelCalls() * 2);
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = webhook.getStartingKey();
        if (null == startingKey) {
            startingKey = new ContentKey();
        }
        ContentPath contentPath = clusterCacheDao.get(webhook.getName(), startingKey, WEBHOOK_LAST_COMPLETED);
        log.debug("getStartingPath startingKey {} contentPath {}", startingKey, contentPath);
        return contentPath;
    }

    @Override
    public ContentPath getLastCompleted() {
        return clusterCacheDao.getOrNull(webhook.getName(), WEBHOOK_LAST_COMPLETED);
    }

    @Override
    public ObjectNode createResponse(ContentPath contentPath) {
        final ObjectNode response = objectMapper.createObjectNode();
        response.put("name", webhook.getName());
        if (contentPath instanceof ContentKey) {
            final ArrayNode uris = response.putArray("uris");
            uris.add(webhook.getChannelUrl() + "/" + contentPath.toUrl());
            response.put("type", "item");
        } else {
            response.put("id", contentPath.toUrl());
            response.put("type", "heartbeat");
        }
        return response;
    }

    @Override
    public ContentPath inProcess(ContentPath contentPath) {
        return contentPath;
    }

    public Optional<ContentPath> next() throws Exception {
        final Exception e = exceptionReference.get();
        if (e != null) {
            log.error("unable to determine next " + webhook.getName(), e);
            throw e;
        }
        return Optional.ofNullable(queue.poll(1, TimeUnit.SECONDS));
    }

    public void start(Webhook webhook, ContentPath startingPath) {
        channel = webhook.getChannelName();
        queryGenerator = new QueryGenerator(startingPath.getTime(), channel);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("single-webhook-" + webhook.getName() + "-%s").build();
        executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {

            ContentPath lastAdded = startingPath;

            @Override
            public void run() {
                try {
                    while (!shouldExit.get()) {
                        if (!doWork()) {
                            Sleeper.sleep(1000);
                        }
                    }
                } catch (InterruptedException | RuntimeInterruptedException e) {
                    exceptionReference.set(e);
                    log.warn("InterruptedException with " + webhook.getName());
                    Thread.currentThread().interrupt();
                } catch (NoSuchChannelException e) {
                    exceptionReference.set(e);
                    log.warn("NoSuchChannelException for " + webhook.getName());
                } catch (Exception e) {
                    exceptionReference.set(e);
                    log.warn("unexpected issue with " + webhook.getName(), e);
                }
            }

            private boolean doWork() throws InterruptedException {
                ActiveTraces.start("SingleWebhookStrategy", webhook);
                try {
                    DateTime latestStableInChannel = TimeUtil.stable();
                    if (!contentRetriever.isLiveChannel(channel)) {
                        latestStableInChannel = contentRetriever.getLastUpdated(channel, MinutePath.NONE).getTime();
                    }
                    final TimeQuery timeQuery = queryGenerator.getQuery(latestStableInChannel);
                    if (timeQuery != null) {
                        addKeys(contentRetriever.queryByTime(timeQuery));
                        if (webhook.isHeartbeat() && queryGenerator.getLastQueryTime().getSecondOfMinute() == 0) {
                            MinutePath minutePath = new MinutePath(queryGenerator.getLastQueryTime().minusMinutes(1));
                            log.debug("sending heartbeat {}", minutePath);
                            addKey(minutePath);
                        }
                        return true;
                    }
                    return false;
                } finally {
                    ActiveTraces.end();
                }
            }

            private void addKeys(Collection<ContentKey> keys) throws InterruptedException {
                log.debug("channel {} keys {}", channel, keys);
                if (log.isTraceEnabled()) {
                    ActiveTraces.getLocal().log(log);
                }
                for (ContentKey key : keys) {
                    addKey(key);
                }
            }

            private void addKey(ContentPath key) throws InterruptedException {
                if (key.compareTo(lastAdded) > 0) {
                    queue.put(key);
                    lastAdded = key;
                }
            }
        });
    }

    @Override
    public void close() {
        WebhookStrategy.close(shouldExit, executorService, queue);
    }
}
