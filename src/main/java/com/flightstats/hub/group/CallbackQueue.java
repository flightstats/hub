package com.flightstats.hub.group;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Every second (with a second lag) we should query new item ids for a channel
 * item ids are put onto a queue, with the queue being size limited to prevent resource explosion
 */
@SuppressWarnings({"Convert2Lambda", "Convert2streamapi"})
public class CallbackQueue implements AutoCloseable {

    private final static Logger logger = LoggerFactory.getLogger(CallbackQueue.class);

    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private BlockingQueue<ContentKey> queue = new ArrayBlockingQueue<>(1000);
    private String channel;
    private DateTime lastQueryTime;

    @Inject
    public CallbackQueue(ChannelService channelService) {
        this.channelService = channelService;
    }

    public Optional<ContentKey> next() {
        try {
            return Optional.fromNullable(queue.poll(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void start(Group group, ContentKey startingKey) {
        lastQueryTime = startingKey.getTime();
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("group-" + group.getName() + "-queue-%s").build();
        ExecutorService executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    doWork();
                } catch (Exception e) {
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private void doWork() {
                while (!shouldExit.get()) {
                    DateTime latestStableInChannel = getLatestStable();
                    logger.trace("iterating {} last={} stable={} ", channel, lastQueryTime, latestStableInChannel);
                    if (lastQueryTime.isBefore(latestStableInChannel)) {
                        TimeUtil.Unit unit = getStepUnit(latestStableInChannel);
                        logger.trace("query {} unit={} lastQueryTime={}", channel, unit, lastQueryTime);
                        TimeQuery query = TimeQuery.builder()
                                .channelName(channel)
                                .startTime(lastQueryTime)
                                .unit(unit).build();
                        query.trace(false);
                        addKeys(channelService.queryByTime(query));
                        lastQueryTime = lastQueryTime.plus(unit.getDuration());
                    } else {
                        Sleeper.sleep(1000);
                    }
                }
            }

            private void addKeys(Collection<ContentKey> keys) {
                logger.trace("channel {} keys {}", channel, keys);
                try {
                    for (ContentKey key : keys) {
                        if (key.compareTo(startingKey) > 0) {
                            queue.put(key);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("InterruptedException " + e.getMessage());
                    throw new RuntimeInterruptedException(e);
                }
            }
        });

    }

    private TimeUtil.Unit getStepUnit(DateTime latestStableInChannel) {
        if (lastQueryTime.isBefore(latestStableInChannel.minusHours(2))) {
            return TimeUtil.Unit.HOURS;
        } else if (lastQueryTime.isBefore(latestStableInChannel.minusMinutes(2))) {
            return TimeUtil.Unit.MINUTES;
        }
        return TimeUtil.Unit.SECONDS;
    }

    private DateTime getLatestStable() {
        if (channelService.isReplicating(channel)) {
            Optional<ContentKey> latest = channelService.getLatest(channel, true);
            if (latest.isPresent()) {
                return latest.get().getTime();
            } else {
                return new DateTime(0);
            }
        } else {
            return TimeUtil.stable();
        }
    }

    @Override
    public void close() {
        if (!shouldExit.get()) {
            shouldExit.set(true);
        }
    }
}
