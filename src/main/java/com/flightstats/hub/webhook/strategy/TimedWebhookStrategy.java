package com.flightstats.hub.webhook.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.Webhook;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.flightstats.hub.constant.ZookeeperNodes.WEBHOOK_LAST_COMPLETED;

@Slf4j
class TimedWebhookStrategy implements WebhookStrategy {
    private AtomicReference<Exception> exceptionReference = new AtomicReference<>();
    private AtomicBoolean shouldExit = new AtomicBoolean(false);

    private final ContentRetriever contentRetriever;
    private final ClusterCacheDao clusterCacheDao;
    private final ObjectMapper objectMapper;
    private final Webhook webhook;

    private final ThreadFactory threadFactory;
    private BlockingQueue<ContentPathKeys> queue;
    private String channel;
    private ScheduledExecutorService executorService;

    // time unit specific functions
    private TimeUtil.Unit unit;
    private int period;
    private Supplier<Integer> getOffsetSeconds;
    private BiFunction<DateTime, Collection<ContentKey>, ContentPathKeys> getContentPathKeys;
    private Supplier<ContentPath> getEmptyPath;
    private Function<ContentPath, DateTime> getReplicatingStable;
    private Function<DateTime, DateTime> getNextTime;
    private Duration duration;

    TimedWebhookStrategy(ContentRetriever contentRetriever,
                         ClusterCacheDao clusterCacheDao,
                         ObjectMapper objectMapper,
                         Webhook webhook) {
        this.webhook = webhook;
        this.contentRetriever = contentRetriever;
        this.clusterCacheDao = clusterCacheDao;
        this.objectMapper = objectMapper;

        this.channel = webhook.getChannelName();
        this.queue = new ArrayBlockingQueue<>(webhook.getParallelCalls() * 2);
        this.threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(webhook.getBatch() + "-webhook-" + webhook.getName() + "-%s")
                .build();

        if (webhook.isSecond()) {
            secondConfig();
        } else {
            minuteConfig();
        }
    }

    static DateTime replicatingStable_minute(ContentPath contentPath) {
        TimeUtil.Unit unit = TimeUtil.Unit.MINUTES;
        if (contentPath instanceof SecondPath) {
            SecondPath secondPath = (SecondPath) contentPath;
            if (secondPath.getTime().getSecondOfMinute() < 59) {
                return unit.round(contentPath.getTime().minusMinutes(1));
            }
        } else if (contentPath instanceof ContentKey) {
            return unit.round(contentPath.getTime().minusMinutes(1));
        }
        return unit.round(contentPath.getTime());
    }

    private static String getBulkUrl(String channelUrl, ContentPath path, String parameter) {
        return channelUrl + "/" + path.toUrl() + "?" + parameter + "=true";
    }

    private void minuteConfig() {
        getOffsetSeconds = () -> (66 - (new DateTime().getSecondOfMinute())) % 60;
        period = 60;
        unit = TimeUtil.Unit.MINUTES;
        duration = unit.getDuration();

        getContentPathKeys = MinutePath::new;
        getEmptyPath = () -> MinutePath.NONE;
        getReplicatingStable = TimedWebhookStrategy::replicatingStable_minute;
        getNextTime = (currentTime) -> currentTime.plus(unit.getDuration());
    }

    private void secondConfig() {
        getOffsetSeconds = () -> 0;
        period = 1;
        unit = TimeUtil.Unit.SECONDS;
        duration = unit.getDuration();

        getContentPathKeys = SecondPath::new;
        getEmptyPath = () -> SecondPath.NONE;
        getReplicatingStable = ContentPath::getTime;
        getNextTime = (currentTime) -> currentTime.plus(unit.getDuration());
    }

    private void determineStrategy(DateTime currentTime) {
        if (webhook.isMinute()) return;
        if (shouldFastForward(currentTime)) {
            minuteConfig();
        } else {
            secondConfig();
        }
    }

    private boolean shouldFastForward(DateTime currentTime) {
        // arbitrarily picked 4 minutes as the fast forward threshold
        return webhook.isFastForwardable()
                && webhook.isSecond()  //only can fast forward type second to type minute
                && Math.abs(Minutes.minutesBetween(stableTime(), currentTime).getMinutes()) > 4;
    }

    private DateTime stableTime() {
        return TimeUtil.stable().minus(unit.getDuration());
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = webhook.getStartingKey();
        if (null == startingKey) {
            startingKey = WebhookStrategy.createContentPath(webhook);
        }
        return clusterCacheDao.get(webhook.getName(), startingKey, WEBHOOK_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return clusterCacheDao.getOrNull(webhook.getName(), WEBHOOK_LAST_COMPLETED);
    }

    @VisibleForTesting
    Runnable getContentKeyGenerator(Webhook webhook, ContentPath initialStartingPath) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (!shouldExit.get()) {
                        doWork();
                    }
                } catch (InterruptedException | RuntimeInterruptedException e) {
                    exceptionReference.set(e);
                    log.warn("InterruptedException with {}", webhook.getName());
                    Thread.currentThread().interrupt();
                } catch (NoSuchChannelException e) {
                    exceptionReference.set(e);
                    log.error("NoSuchChannelException for {}", webhook.getName());
                } catch (Exception e) {
                    exceptionReference.set(e);
                    log.error("unexpected issue with {}", webhook.getName(), e);
                }
            }
            ContentPath lastAdded = initialStartingPath;

            private void doWork() throws InterruptedException {
                DateTime timeBeingWorkedOn = getNextTime();
                DateTime stableTime = getStableTime();
                log.debug("webhook: {} lastAddedPath {} timeBeingWorkedOn {} stable {}",
                        webhook, initialStartingPath, timeBeingWorkedOn, stableTime);

                while (timeBeingWorkedOn.isBefore(stableTime)) {
                    log.debug("working on {} {}", channel, timeBeingWorkedOn);
                    try {
                        ActiveTraces.start("TimedWebhookStrategy.doWork", webhook);
                        Collection<ContentKey> keys = queryKeys(timeBeingWorkedOn)
                                .stream()
                                .filter(key -> key.compareTo(lastAdded) > 0)
                                .collect(Collectors.toCollection(ArrayList::new));

                        ContentPathKeys pathAndKeys = getContentPathKeys.apply(timeBeingWorkedOn, keys);
                        log.trace("results {} {} {}", channel, pathAndKeys, pathAndKeys.getKeys());
                        queue.put(pathAndKeys);

                        lastAdded = pathAndKeys;
                        determineStrategy(lastAdded.getTime());
                        timeBeingWorkedOn = getNextTime();
                    } finally {
                        ActiveTraces.end();
                    }
                }
            }

            private DateTime getStableTime() {
                DateTime defaultStableTime = TimeUtil.stable().minus(duration);
                if (!contentRetriever.isLiveChannel(channel)) {
                    ContentPath lastUpdatePath = contentRetriever.getLastUpdated(channel, getEmptyPath.get());
                    DateTime replicatedStable = getReplicatingStable.apply(lastUpdatePath);
                    log.debug("{}: replicating {} replicated stable {}", webhook, lastUpdatePath, replicatedStable);
                    if (replicatedStable.isBefore(defaultStableTime)) {
                        return replicatedStable;
                    }
                }
                return defaultStableTime;
            }

            private DateTime getNextTime() {
                DateTime lastTime = lastAdded.getTime();
                if (lastAdded instanceof ContentKey) {
                    return lastTime;
                }
                return getNextTime.apply(lastTime);
            }
        };
    }

    @Override
    public void start(Webhook webhook, ContentPath initialStartingPath) {
        executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        log.info("starting {} with starting path {}", webhook, initialStartingPath);
        executorService.scheduleAtFixedRate(
                getContentKeyGenerator(webhook, initialStartingPath),
                getOffsetSeconds.get(),
                period,
                TimeUnit.SECONDS);
    }

    private Collection<ContentKey> queryKeys(DateTime time) {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channel)
                .startTime(time)
                .unit(unit)
                .stable(true)
                .epoch(Epoch.IMMUTABLE)
                .build();
        return contentRetriever.queryByTime(timeQuery);
    }

    @Override
    public Optional<ContentPath> next() throws Exception {
        Exception e = exceptionReference.get();
        if (e != null) {
            log.error("unable to determine next {}", webhook.getName(), e);
            throw e;
        }
        return Optional.ofNullable(queue.poll(10, TimeUnit.MINUTES));
    }

    @VisibleForTesting
    SortedSet<ContentPathKeys> drainQueue() {
        List<ContentPathKeys> contentPaths = new ArrayList<>();
        queue.drainTo(contentPaths);
        return new TreeSet<>(contentPaths);
    }

    @Override
    public ObjectNode createResponse(ContentPath contentPath) {
        ObjectNode response = objectMapper.createObjectNode();
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
        return getContentPathKeys.apply(contentPath.getTime(), queryKeys(contentPath.getTime()));
    }

    @Override
    public void close() {
        WebhookStrategy.close(shouldExit, executorService, queue);
    }
}
