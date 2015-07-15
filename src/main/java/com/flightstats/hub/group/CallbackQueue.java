package com.flightstats.hub.group;

import com.flightstats.hub.dao.ChannelService;
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
        queryGenerator = new QueryGenerator(startingKey.getTime(), channel);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("group-" + group.getName() + "-queue-%s").build();
        ExecutorService executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {

            ContentKey lastAdded = startingKey;
            int missed = 0;

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
                        logger.trace("query {}", timeQuery);
                        addKeys(channelService.queryByTime(timeQuery));
                    }
                }
            }

            private void handleReplication() {
                Collection<ContentKey> keys = Collections.EMPTY_LIST;
                Optional<ContentKey> latest = channelService.getLatest(channel, true, false);
                if (latest.isPresent()) {
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
                addKeys(keys);
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
                if (keys.isEmpty()) {
                    if (missed < 4) {
                        missed++;
                    }
                    int millis = 1000 * missed ^ 2;
                    logger.trace("channel {} sleeping for {} millis", channel, millis);
                    Sleeper.sleep(millis);
                } else {
                    missed = 0;
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
