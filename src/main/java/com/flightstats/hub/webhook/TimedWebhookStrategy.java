package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

class TimedWebhookStrategy implements WebhookStrategy {

    private final static Logger logger = LoggerFactory.getLogger(TimedWebhookStrategy.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final Webhook webhook;
    private final TimedWebhook timedWebhook;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<ContentPathKeys> queue;
    private String channel;
    private ScheduledExecutorService executorService;

    TimedWebhookStrategy(Webhook webhook, LastContentPath lastContentPath, ChannelService channelService) {
        this.webhook = webhook;
        this.timedWebhook = TimedWebhook.getTimedWebhook(webhook);
        channel = ChannelNameUtils.extractFromChannelUrl(webhook.getChannelUrl());
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(webhook.getParallelCalls() * 2);
    }

    private static String getBulkUrl(String channelUrl, ContentPath path, String parameter) {
        return channelUrl + "/" + path.toUrl() + "?" + parameter + "=true";
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = webhook.getStartingKey();
        if (null == startingKey) {
            startingKey = WebhookStrategy.createContentPath(webhook);
        }
        return lastContentPath.get(webhook.getName(), startingKey, WebhookLeader.WEBHOOK_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return lastContentPath.getOrNull(webhook.getName(), WebhookLeader.WEBHOOK_LAST_COMPLETED);
    }

    @Override
    public void start(Webhook webhook, ContentPath startingPath) {
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat(webhook.getBatch() + "-webhook-" + webhook.getName() + "-%s").build();
        executorService = Executors.newSingleThreadScheduledExecutor(factory);
        logger.info("starting {} with offset {}", webhook, timedWebhook.getOffsetSeconds());
        executorService.scheduleAtFixedRate(new Runnable() {

            ContentPath lastAdded = startingPath;
            ChannelConfig channelConfig = channelService.getChannelConfig(channel, true);

            @Override
            public void run() {
                try {
                    if (!shouldExit.get()) {
                        doWork();
                    }
                } catch (InterruptedException | RuntimeInterruptedException e) {
                    error.set(true);
                    logger.info("InterruptedException with " + channel);
                } catch (Exception e) {
                    error.set(true);
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private void doWork() throws InterruptedException {
                Duration duration = timedWebhook.getUnit().getDuration();
                DateTime nextTime = lastAdded.getTime().plus(duration);
                if (lastAdded instanceof ContentKey) {
                    nextTime = lastAdded.getTime();
                }
                DateTime stable = TimeUtil.stable().minus(duration);
                if (!channelConfig.isLive()) {
                    ContentPath contentPath = channelService.getLastUpdated(channel, timedWebhook.getNone());
                    DateTime replicatedStable = timedWebhook.getReplicatingStable(contentPath);
                    if (replicatedStable.isBefore(stable)) {
                        stable = replicatedStable;
                    }
                    logger.debug("replicating {} stable {}", contentPath, stable);
                }
                logger.debug("lastAdded {} nextTime {} stable {}", lastAdded, nextTime, stable);
                while (nextTime.isBefore(stable) || nextTime.isEqual(stable)) {
                    try {
                        ActiveTraces.start("TimedWebhookStrategy.doWork", webhook);
                        Collection<ContentKey> keys = queryKeys(nextTime)
                                .stream()
                                .filter(key -> key.compareTo(lastAdded) > 0)
                                .collect(Collectors.toCollection(ArrayList::new));

                        ContentPathKeys nextPath = timedWebhook.newTime(nextTime, keys);
                        logger.trace("results {} {} {}", channel, nextPath, nextPath.getKeys());
                        queue.put(nextPath);
                        lastAdded = nextPath;
                        nextTime = lastAdded.getTime().plus(duration);
                    } finally {
                        ActiveTraces.end();
                    }
                }
            }

        }, timedWebhook.getOffsetSeconds(), timedWebhook.getPeriodSeconds(), TimeUnit.SECONDS);
    }

    private Collection<ContentKey> queryKeys(DateTime time) {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channel)
                .startTime(time)
                .unit(timedWebhook.getUnit())
                .stable(true)
                .build();
        return channelService.queryByTime(timeQuery);
    }

    @Override
    public Optional<ContentPath> next() {
        if (error.get()) {
            throw new RuntimeException("unable to determine next");
        }
        try {
            return Optional.fromNullable(queue.poll(10, TimeUnit.MINUTES));
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    @Override
    public ObjectNode createResponse(ContentPath contentPath) {
        ObjectNode response = mapper.createObjectNode();
        response.put("name", webhook.getName());
        String url = contentPath.toUrl();
        response.put("id", url);
        String channelUrl = webhook.getChannelUrl();
        response.put("url", channelUrl + "/" + url);
        response.put("batchUrl", getBulkUrl(channelUrl, contentPath, "batch"));
        response.put("bulkUrl", getBulkUrl(channelUrl, contentPath, "bulk"));
        ArrayNode uris = response.putArray("uris");
        Collection<ContentKey> keys = ((ContentPathKeys) contentPath).getKeys();
        for (ContentKey key : keys) {
            uris.add(channelUrl + "/" + key.toUrl());
        }
        if (keys.isEmpty()) {
            response.put("type", "heartbeat");
        } else {
            response.put("type", "items");
        }
        return response;
    }

    @Override
    public ContentPath inProcess(ContentPath contentPath) {
        return timedWebhook.newTime(contentPath.getTime(), queryKeys(contentPath.getTime()));
    }

    @Override
    public void close() throws Exception {
        WebhookStrategy.close(shouldExit, executorService, queue);
    }
}
