package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.ClusterStateDao;
import com.flightstats.hub.dao.ChannelService;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

class SingleWebhookStrategy implements WebhookStrategy {

    private final static Logger logger = LoggerFactory.getLogger(SingleWebhookStrategy.class);
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final Webhook webhook;
    private final ClusterStateDao clusterStateDao;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicReference<Exception> exceptionReference = new AtomicReference<>();
    private BlockingQueue<ContentPath> queue;
    private String channel;
    private QueryGenerator queryGenerator;
    private ExecutorService executorService;


    SingleWebhookStrategy(Webhook webhook, ClusterStateDao clusterStateDao, ChannelService channelService) {
        this.webhook = webhook;
        this.clusterStateDao = clusterStateDao;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(webhook.getParallelCalls() * 2);
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = webhook.getStartingKey();
        if (null == startingKey) {
            startingKey = new ContentKey();
        }
        ContentPath contentPath = clusterStateDao.get(webhook.getName(), startingKey, WebhookLeader.WEBHOOK_LAST_COMPLETED);
        logger.info("getStartingPath startingKey {} contentPath {}", startingKey, contentPath);
        return contentPath;
    }

    @Override
    public ContentPath getLastCompleted() {
        return clusterStateDao.getOrNull(webhook.getName(), WebhookLeader.WEBHOOK_LAST_COMPLETED);
    }

    @Override
    public ObjectNode createResponse(ContentPath contentPath) {
        ObjectNode response = mapper.createObjectNode();
        response.put("name", webhook.getName());
        if (contentPath instanceof ContentKey) {
            ArrayNode uris = response.putArray("uris");
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
        Exception e = exceptionReference.get();
        if (e != null) {
            logger.error("unable to determine next " + webhook.getName(), e);
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
                    logger.info("InterruptedException with " + webhook.getName());
                    Thread.currentThread().interrupt();
                } catch (NoSuchChannelException e) {
                    exceptionReference.set(e);
                    logger.debug("NoSuchChannelException for " + webhook.getName());
                } catch (Exception e) {
                    exceptionReference.set(e);
                    logger.warn("unexpected issue with " + webhook.getName(), e);
                }
            }

            private boolean doWork() throws InterruptedException {
                ActiveTraces.start("SingleWebhookStrategy", webhook);
                try {
                    DateTime latestStableInChannel = TimeUtil.stable();
                    if (!channelService.isLiveChannel(channel)) {
                        latestStableInChannel = channelService.adjustLastUpdatePathIfReplicating(channel, MinutePath.NONE).getTime();
                    }
                    TimeQuery timeQuery = queryGenerator.getQuery(latestStableInChannel);
                    if (timeQuery != null) {
                        addKeys(channelService.queryByTime(timeQuery));
                        if (webhook.isHeartbeat() && queryGenerator.getLastQueryTime().getSecondOfMinute() == 0) {
                            MinutePath minutePath = new MinutePath(queryGenerator.getLastQueryTime().minusMinutes(1));
                            logger.debug("sending heartbeat {}", minutePath);
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
                logger.debug("channel {} keys {}", channel, keys);
                if (logger.isTraceEnabled()) {
                    ActiveTraces.getLocal().log(logger);
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
