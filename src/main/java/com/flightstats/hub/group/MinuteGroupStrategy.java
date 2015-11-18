package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MinuteGroupStrategy implements GroupStrategy {

    private final static Logger logger = LoggerFactory.getLogger(MinuteGroupStrategy.class);

    private final Group group;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<MinutePath> queue;
    private String channel;
    private ScheduledExecutorService executorService;

    public MinuteGroupStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        this.group = group;
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(group.getParallelCalls() * 2);
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = group.getStartingKey();
        if (null == startingKey) {
            startingKey = new MinutePath();
        }
        return lastContentPath.get(group.getName(), startingKey, GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return lastContentPath.getOrNull(group.getName(), GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public void start(Group group, ContentPath startingPath) {
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
                } catch (InterruptedException | RuntimeInterruptedException e) {
                    error.set(true);
                    logger.info("InterruptedException with " + channel);
                } catch (Exception e) {
                    error.set(true);
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private void doWork() throws InterruptedException {
                ActiveTraces.start("MinuteGroupStrategy.doWork", group);
                try {
                    DateTime nextTime = lastAdded.getTime().plusMinutes(1);
                    if (lastAdded instanceof ContentKey) {
                        nextTime = lastAdded.getTime();
                    }
                    DateTime stable = TimeUtil.stable().minusMinutes(1);
                    if (channelService.isReplicating(channel)) {
                        ContentPath contentPath = lastContentPath.get(channel, MinutePath.NONE, ChannelReplicator.REPLICATED_LAST_UPDATED);
                        stable = contentPath.getTime().plusSeconds(1);
                        logger.debug("replicating {} stable {}", contentPath, stable);
                    }
                    logger.debug("lastAdded {} nextTime {} stable {}", lastAdded, nextTime, stable);
                    while (nextTime.isBefore(stable)) {
                        Collection<ContentKey> keys = queryKeys(nextTime)
                                .stream()
                                .filter(key -> key.compareTo(lastAdded) > 0)
                                .collect(Collectors.toCollection(ArrayList::new));
                        MinutePath nextPath = new MinutePath(nextTime, keys);
                        logger.trace("results {} {} {}", channel, nextPath, nextPath.getKeys());
                        ActiveTraces.getLocal().add("MinuteGroupStrategy.doWork nextPath", nextPath);
                        queue.put(nextPath);
                        lastAdded = nextPath;
                        nextTime = lastAdded.getTime().plusMinutes(1);
                    }
                } finally {
                    ActiveTraces.end();
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

    private Collection<ContentKey> queryKeys(DateTime time) {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channel)
                .startTime(time)
                .unit(TimeUtil.Unit.MINUTES)
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
        response.put("batchUrl", getBatchUrl(channelUrl, minutePath));
        ArrayNode uris = response.putArray("uris");
        Collection<ContentKey> keys = minutePath.getKeys();
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

    public static String getBatchUrl(String channelUrl, MinutePath path) {
        return channelUrl + "/" + path.toUrl() + "?batch=true";
    }

    @Override
    public ContentPath inProcess(ContentPath contentPath) {
        return new MinutePath(contentPath.getTime(), queryKeys(contentPath.getTime()));
    }

    @Override
    public void close() throws Exception {
        GroupStrategy.close(shouldExit, executorService, queue);
    }
}
