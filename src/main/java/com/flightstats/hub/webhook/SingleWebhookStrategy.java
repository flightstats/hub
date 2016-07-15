package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

class SingleWebhookStrategy implements WebhookStrategy {

    private final static Logger logger = LoggerFactory.getLogger(SingleWebhookStrategy.class);
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final Webhook webhook;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<ContentPath> queue;
    private String channel;
    private QueryGenerator queryGenerator;
    private ExecutorService executorService;


    SingleWebhookStrategy(Webhook webhook, LastContentPath lastContentPath, ChannelService channelService) {
        this.webhook = webhook;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(webhook.getParallelCalls() * 2);
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = webhook.getStartingKey();
        if (null == startingKey) {
            startingKey = new ContentKey();
        }
        return lastContentPath.get(webhook.getName(), startingKey, WebhookLeader.WEBHOOK_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return lastContentPath.getOrNull(webhook.getName(), WebhookLeader.WEBHOOK_LAST_COMPLETED);
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

    public Optional<ContentPath> next() {
        if (error.get()) {
            logger.error("unable to determine next");
        }
        try {
            return Optional.fromNullable(queue.poll(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void start(Webhook webhook, ContentPath startingPath) {
        channel = webhook.getChannelName();
        queryGenerator = new QueryGenerator(startingPath.getTime(), channel);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("single-webhook-" + webhook.getName() + "-%s").build();
        executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {

            ContentPath lastAdded = startingPath;
            ChannelConfig channelConfig = channelService.getChannelConfig(channel, true);

            @Override
            public void run() {
                try {
                    while (!shouldExit.get()) {
                        if (!doWork()) {
                            Sleeper.sleep(1000);
                        }
                    }
                } catch (InterruptedException | RuntimeInterruptedException e) {
                    error.set(true);
                    logger.info("InterruptedException with " + channel);
                } catch (NoSuchChannelException e) {
                    error.set(true);
                    logger.debug("NoSuchChannelException for " + channel);
                } catch (Exception e) {
                    error.set(true);
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private boolean doWork() throws InterruptedException {
                ActiveTraces.start("SingleWebhookStrategy", webhook);
                try {
                    DateTime latestStableInChannel = TimeUtil.stable();
                    if (!channelConfig.isLive()) {
                        latestStableInChannel = channelService.getLastUpdated(channel, MinutePath.NONE).getTime();
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
