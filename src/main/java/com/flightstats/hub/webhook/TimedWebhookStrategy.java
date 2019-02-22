package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
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

class TimedWebhookStrategy implements WebhookStrategy {

    private final static Logger logger = LoggerFactory.getLogger(TimedWebhookStrategy.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final Webhook webhook;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicReference<Exception> exceptionReference = new AtomicReference<>();
    private BlockingQueue<ContentPathKeys> queue;
    private String channel;
    private ScheduledExecutorService executorService;

    // time unit specific functions
    private TimeUtil.Unit unit;
    private int period;
    private Supplier<Integer> getOffsetSeconds;
    private BiFunction<DateTime, Collection<ContentKey>, ContentPathKeys> newTime;
    private Supplier<ContentPath> getNone;
    private Function<ContentPath, DateTime> getReplicatingStable;
    private Function<DateTime, DateTime> getNextTime;
    private Duration duration;

    TimedWebhookStrategy(Webhook webhook, LastContentPath lastContentPath, ChannelService channelService) {
        this.webhook = webhook;
        this.channel = webhook.getChannelName();
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(webhook.getParallelCalls() * 2);
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

        newTime = MinutePath::new;
        getNone = () -> MinutePath.NONE;
        getReplicatingStable = TimedWebhookStrategy::replicatingStable_minute;
        getNextTime = (currentTime) -> currentTime.plus(unit.getDuration());
    }

    private void secondConfig() {
        getOffsetSeconds = () -> 0;
        period = 1;
        unit = TimeUtil.Unit.SECONDS;
        duration = unit.getDuration();

        newTime = SecondPath::new;
        getNone = () -> SecondPath.NONE;
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
        logger.info("starting {} with starting path {}", webhook, startingPath);
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
                    exceptionReference.set(e);
                    logger.info("InterruptedException with " + webhook.getName());
                } catch (NoSuchChannelException e) {
                    exceptionReference.set(e);
                    logger.debug("NoSuchChannelException for " + webhook.getName());
                } catch (Exception e) {
                    exceptionReference.set(e);
                    logger.warn("unexpected issue with " + webhook.getName(), e);
                }
            }

            private void doWork() throws InterruptedException {
                DateTime nextTime = getNextTime.apply(lastAdded.getTime());
                if (lastAdded instanceof ContentKey) {
                    nextTime = lastAdded.getTime();
                }
                DateTime stable = TimeUtil.stable().minus(duration);
                if (!channelConfig.isLive()) {
                    ContentPath contentPath = channelService.getLastUpdated(channel, getNone.get());
                    DateTime replicatedStable = getReplicatingStable.apply(contentPath);
                    if (replicatedStable.isBefore(stable)) {
                        stable = replicatedStable;
                    }
                    logger.debug("replicating {} stable {}", contentPath, stable);
                }
                logger.debug("lastAdded {} nextTime {} stable {}", lastAdded, nextTime, stable);
                while (nextTime.isBefore(stable)) {
                    try {
                        ActiveTraces.start("TimedWebhookStrategy.doWork", webhook);
                        Collection<ContentKey> keys = queryKeys(nextTime)
                                .stream()
                                .filter(key -> key.compareTo(lastAdded) > 0)
                                .collect(Collectors.toCollection(ArrayList::new));

                        ContentPathKeys nextPath = newTime.apply(nextTime, keys);
                        logger.trace("results {} {} {}", channel, nextPath, nextPath.getKeys());
                        queue.put(nextPath);
                        lastAdded = nextPath;
                        determineStrategy(lastAdded.getTime());
                        nextTime = getNextTime.apply(lastAdded.getTime());
                    } finally {
                        ActiveTraces.end();
                    }
                }
            }

        }, getOffsetSeconds.get(), period, TimeUnit.SECONDS);
    }

    private Collection<ContentKey> queryKeys(DateTime time) {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channel)
                .startTime(time)
                .unit(unit)
                .stable(true)
                .epoch(Epoch.IMMUTABLE)
                .build();
        return channelService.queryByTime(timeQuery);
    }

    @Override
    public Optional<ContentPath> next() throws Exception {
        Exception e = exceptionReference.get();
        if (e != null) {
            logger.error("unable to determine next " + webhook.getName(), e);
            throw e;
        }
        return Optional.ofNullable(queue.poll(10, TimeUnit.MINUTES));
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
        return newTime.apply(contentPath.getTime(), queryKeys(contentPath.getTime()));
    }

    @Override
    public void close() throws Exception {
        WebhookStrategy.close(shouldExit, executorService, queue);
    }
}
