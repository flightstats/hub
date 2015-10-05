package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicatorImpl;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinuteGroupStrategy implements GroupStrategy {

    private final static Logger logger = LoggerFactory.getLogger(MinuteGroupStrategy.class);

    private final Group group;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<MinutePath> queue = new ArrayBlockingQueue<>(1000);
    private String channel;
    private ScheduledExecutorService executorService;

    public MinuteGroupStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        this.group = group;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = group.getStartingKey();
        if (null == startingKey) {
            startingKey = new MinutePath();
        }
        return getLastCompleted(startingKey);
    }

    private ContentPath getLastCompleted(ContentPath defaultKey) {
        return lastContentPath.get(group.getName(), defaultKey, GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return getLastCompleted(MinutePath.NONE);
    }

    @Override
    public void start(Group group, ContentPath startingPath) {
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("minute-group-" + group.getName() + "-%s").build();
        executorService = Executors.newSingleThreadScheduledExecutor(factory);
        int offset = getOffset();
        logger.info("starting {} with offset {}", group, offset);
        executorService.scheduleAtFixedRate(new Runnable() {

            ContentPath lastAdded = startingPath;

            @Override
            public void run() {
                try {
                    if (!shouldExit.get()) {
                        doWork();
                    }
                } catch (Exception e) {
                    error.set(true);
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private void doWork() {
                try {
                    DateTime nextTime = lastAdded.getTime().plusMinutes(1);
                    DateTime stable = TimeUtil.stable().minusMinutes(1);
                    if (channelService.isReplicating(channel)) {
                        ContentPath contentPath = lastContentPath.get(channel, MinutePath.NONE, ChannelReplicatorImpl.REPLICATED_LAST_UPDATED);
                        //todo - gfm - 10/5/15 - this could be a ContentKey, what if it is?
                        stable = contentPath.getTime().plusSeconds(1);
                    }
                    logger.debug("lastAdded {} nextTime {} stable {}", lastAdded, nextTime, stable);
                    while (nextTime.isBefore(stable)) {
                        MinutePath nextPath = createMinutePath(nextTime);
                        logger.trace("results {} {} {}", channel, nextPath, nextPath.getKeys());
                        queue.put(nextPath);
                        lastAdded = nextPath;
                        nextTime = lastAdded.getTime().plusMinutes(1);
                    }
                } catch (InterruptedException e) {
                    logger.info("InterruptedException " + channel + " " + e.getMessage());
                    throw new RuntimeInterruptedException(e);
                }
            }

        }, getOffset(), 60, TimeUnit.SECONDS);
    }

    private int getOffset() {
        int secondOfMinute = new DateTime().getSecondOfMinute();
        if (secondOfMinute < 6) {
            return 6 - secondOfMinute;
        } else if (secondOfMinute > 6) {
            return 66 - secondOfMinute;
        }
        return 0;
    }

    private MinutePath createMinutePath(DateTime time) {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channel)
                .startTime(time)
                .unit(TimeUtil.Unit.MINUTES)
                .stable(true)
                .traces(Traces.NOOP)
                .build();
        return new MinutePath(time, channelService.queryByTime(timeQuery));
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
    public ContentPath createContentPath() {
        return MinutePath.NONE;
    }

    @Override
    public ObjectNode createResponse(ContentPath contentPath, ObjectMapper mapper) {
        MinutePath minutePath = (MinutePath) contentPath;
        ObjectNode response = mapper.createObjectNode();
        response.put("name", group.getName());
        String url = contentPath.toUrl();
        response.put("id", url);
        String channelUrl = group.getChannelUrl();
        response.put("url", channelUrl + "/" + url);
        response.put("batchUrl", channelUrl + "/" + url + "?batch=true");
        ArrayNode uris = response.putArray("uris");
        Collection<ContentKey> keys = minutePath.getKeys();
        for (ContentKey key : keys) {
            uris.add(channelUrl + "/" + key.toUrl());
        }
        return response;
    }

    @Override
    public ContentPath inProcess(ContentPath contentPath) {
        return createMinutePath(contentPath.getTime());
    }

    @Override
    public void close() throws Exception {
        if (!shouldExit.get()) {
            shouldExit.set(true);
        }
        if (null != executorService) {
            executorService.shutdown();
        }
    }
}
