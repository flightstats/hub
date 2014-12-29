package com.flightstats.hub.group;

import com.flightstats.hub.dao.ContentService;
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
import org.joda.time.Duration;
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

    private final ContentService contentService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private BlockingQueue<ContentKey> queue = new ArrayBlockingQueue<>(1000);
    private String channel;
    private DateTime lastTime;

    @Inject
    public CallbackQueue(ContentService contentService) {
        this.contentService = contentService;
    }

    public Optional<ContentKey> next() {
        try {
            return Optional.fromNullable(queue.poll(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void start(Group group, ContentKey lastCompletedKey) {
        lastTime = lastCompletedKey.getTime();
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("group-" + group.getName() + "-queue-%s").build();
        ExecutorService executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                //todo - gfm - 12/2/14 - handle exceptions
                //todo - gfm - 12/2/14 - this could change the query units based on lag from now
                while (!shouldExit.get()) {
                    DateTime stableOrdering = TimeUtil.stable();
                    logger.trace("iterating {} last={} stable={} ", channel, lastTime, stableOrdering);
                    if (lastTime.isBefore(stableOrdering)) {
                        //todo - gfm - 12/3/14 - do we want a convenience method that doens't need these params?
                        TimeQuery query = TimeQuery.builder().channelName(channel).startTime(lastTime).unit(TimeUtil.Unit.SECONDS).build();
                        query.trace(false);
                        addKeys(contentService.queryByTime(query));
                        lastTime = lastTime.plusSeconds(1);
                    } else {
                        Duration duration = new Duration(stableOrdering, lastTime);
                        logger.trace("sleeping " + duration.getMillis());
                        Sleeper.sleep(duration.getMillis());
                    }
                }
            }

            private void addKeys(Collection<ContentKey> keys) {
                logger.trace("channel {} keys {}", channel, keys);
                if (lastTime.isAfter(lastCompletedKey.getTime())) {
                    queue.addAll(keys);
                } else {
                    for (ContentKey key : keys) {
                        if (key.compareTo(lastCompletedKey) > 0) {
                            queue.add(key);
                        }
                    }
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
