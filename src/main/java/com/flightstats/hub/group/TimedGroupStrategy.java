package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicator;
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

public class TimedGroupStrategy implements GroupStrategy {

    private final static Logger logger = LoggerFactory.getLogger(TimedGroupStrategy.class);

    private final Group group;
    private final TimedGroup timedGroup;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<ContentPath> queue;
    private String channel;
    private ScheduledExecutorService executorService;

    public TimedGroupStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        this.group = group;
        this.timedGroup = TimedGroup.getTimedGroup(group);
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(group.getParallelCalls() * 2);
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = group.getStartingKey();
        if (null == startingKey) {
            startingKey = GroupStrategy.createContentPath(group);
        }
        return lastContentPath.get(group.getName(), startingKey, GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return lastContentPath.getOrNull(group.getName(), GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public void start(Group group, ContentPath startingPath) {
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat(group.getBatch() + "-group-" + group.getName() + "-%s").build();
        executorService = Executors.newSingleThreadScheduledExecutor(factory);
        logger.info("starting {} with offset {}", group, timedGroup.getOffsetSeconds());
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
                Duration duration = timedGroup.getUnit().getDuration();
                DateTime nextTime = lastAdded.getTime().plus(duration);
                if (lastAdded instanceof ContentKey) {
                    nextTime = lastAdded.getTime();
                }
                DateTime stable = TimeUtil.stable().minus(duration);
                if (channelService.isReplicating(channel)) {
                    ContentPath contentPath = lastContentPath.get(channel, timedGroup.getNone(), ChannelReplicator.REPLICATED_LAST_UPDATED);
                    stable = contentPath.getTime().plusSeconds(1);
                    logger.debug("replicating {} stable {}", contentPath, stable);
                }
                logger.debug("lastAdded {} nextTime {} stable {}", lastAdded, nextTime, stable);
                while (nextTime.isBefore(stable)) {
                    try {
                        ActiveTraces.start("TimedGroupStrategy.doWork", group);
                        Collection<ContentKey> keys = queryKeys(nextTime)
                                .stream()
                                .filter(key -> key.compareTo(lastAdded) > 0)
                                .collect(Collectors.toCollection(ArrayList::new));

                        ContentPath nextPath = timedGroup.newTime(nextTime, keys);
                        logger.trace("results {} {} {}", channel, nextPath, ((Keys) nextPath).getKeys());
                        queue.put(nextPath);
                        lastAdded = nextPath;
                        nextTime = lastAdded.getTime().plus(duration);
                    } finally {
                        ActiveTraces.end();
                    }
                }
            }

        }, timedGroup.getOffsetSeconds(), timedGroup.getPeriodSeconds(), TimeUnit.SECONDS);
    }

    private Collection<ContentKey> queryKeys(DateTime time) {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channel)
                .startTime(time)
                .unit(timedGroup.getUnit())
                .stable(true)
                .location(Location.CACHE)
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
    public ObjectNode createResponse(ContentPath contentPath, ObjectMapper mapper) {
        ObjectNode response = mapper.createObjectNode();
        response.put("name", group.getName());
        String url = contentPath.toUrl();
        response.put("id", url);
        String channelUrl = group.getChannelUrl();
        response.put("url", channelUrl + "/" + url);
        response.put("batchUrl", getBulkUrl(channelUrl, contentPath, "batch"));
        response.put("bulkUrl", getBulkUrl(channelUrl, contentPath, "bulk"));
        ArrayNode uris = response.putArray("uris");
        Collection<ContentKey> keys = ((Keys) contentPath).getKeys();
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

    public static String getBulkUrl(String channelUrl, ContentPath path, String parameter) {
        return channelUrl + "/" + path.toUrl() + "?" + parameter + "=true";
    }

    @Override
    public ContentPath inProcess(ContentPath contentPath) {
        return timedGroup.newTime(contentPath.getTime(), queryKeys(contentPath.getTime()));
    }

    @Override
    public void close() throws Exception {
        GroupStrategy.close(shouldExit, executorService, queue);
    }
}
