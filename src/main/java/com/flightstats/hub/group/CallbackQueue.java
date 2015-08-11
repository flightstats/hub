package com.flightstats.hub.group;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
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
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Every second (with a second lag) we should query new item ids for a channel
 * item ids are put onto a queue, with the queue being size limited to prevent resource explosion
 */
@SuppressWarnings({"Convert2Lambda", "Convert2streamapi"})
public class CallbackQueue implements AutoCloseable {

    private final static Logger logger = LoggerFactory.getLogger(CallbackQueue.class);

    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<ContentKey> queue = new ArrayBlockingQueue<>(1000);
    private String channel;
    private QueryGenerator queryGenerator;

    @Inject
    public CallbackQueue(ChannelService channelService) {
        this.channelService = channelService;

    }

    public Optional<ContentKey> next() {
        if (error.get()) {
            throw new RuntimeException("unable to determine next");
        }
        try {
            return Optional.fromNullable(queue.poll(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void start(Group group, ContentKey startingKey) {
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        ChannelConfig config = channelService.getCachedChannelConfig(channel);
        DateTime earliestTime = TimeUtil.getEarliestTime((int) config.getTtlDays());
        if (startingKey.getTime().isBefore(earliestTime)) {
            logger.info("{} changing starting key {} for earliestTime {}", channel, startingKey, earliestTime);
            startingKey = new ContentKey(earliestTime, "0");
        }
        final ContentKey earliestKey = startingKey;
        queryGenerator = new QueryGenerator(earliestKey.getTime(), channel);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("group-" + group.getName() + "-queue-%s").build();
        ExecutorService executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {

            ContentKey lastAdded = earliestKey;

            @Override
            public void run() {
                try {
                    doWork();
                } catch (Exception e) {
                    error.set(true);
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private void doWork() {
                while (!shouldExit.get()) {
                    if (channelService.isReplicating(channel)) {
                        handleReplication();
                    } else {
                        TimeQuery timeQuery = queryGenerator.getQuery(TimeUtil.stable());
                        if (timeQuery != null) {
                            addKeys(channelService.queryByTime(timeQuery));
                        } else {
                            Sleeper.sleep(1000);
                        }
                    }
                }
            }

            private void handleReplication() {
                Collection<ContentKey> keys = Collections.EMPTY_LIST;
                Optional<ContentKey> latest = channelService.getLatest(channel, true, false);
                if (latest.isPresent() && latest.get().compareTo(lastAdded) > 0) {
                    DirectionQuery query = DirectionQuery.builder()
                            .channelName(channel)
                            .contentKey(lastAdded)
                            .next(true)
                            .stable(true)
                            .ttlDays(channelService.getCachedChannelConfig(channel).getTtlDays())
                            .count(50)
                            .build();
                    query.trace(true);
                    query.getTraces().add("latest", latest.get());
                    keys = channelService.getKeys(query)
                            .stream()
                            .filter(key -> key.compareTo(latest.get()) <= 0)
                            .collect(Collectors.toCollection(TreeSet::new));
                    if (logger.isTraceEnabled()) {
                        query.getTraces().log(logger);
                    }
                }
                if (keys.isEmpty()) {
                    Sleeper.sleep(1000);
                } else {
                    addKeys(keys);
                }
            }

            private void addKeys(Collection<ContentKey> keys) {
                logger.debug("channel {} keys {}", channel, keys);
                try {
                    for (ContentKey key : keys) {
                        if (key.compareTo(lastAdded) > 0) {
                            queue.put(key);
                            lastAdded = key;
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("InterruptedException " + e.getMessage());
                    throw new RuntimeInterruptedException(e);
                }
            }

        });

    }



    @Override
    public void close() {
        if (!shouldExit.get()) {
            shouldExit.set(true);
        }
    }
}
