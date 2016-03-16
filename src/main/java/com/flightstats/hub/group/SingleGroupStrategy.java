package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleGroupStrategy implements GroupStrategy {

    private final static Logger logger = LoggerFactory.getLogger(SingleGroupStrategy.class);

    private final Group group;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<ContentPath> queue;
    private String channel;
    private QueryGenerator queryGenerator;
    private ExecutorService executorService;


    public SingleGroupStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        this.group = group;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.queue = new ArrayBlockingQueue<>(group.getParallelCalls() * 2);
    }

    public ContentPath createContentPath() {
        return ContentKey.NONE;
    }

    @Override
    public ContentPath getStartingPath() {
        ContentPath startingKey = group.getStartingKey();
        if (null == startingKey) {
            startingKey = new ContentKey();
        }
        return lastContentPath.get(group.getName(), startingKey, GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return lastContentPath.getOrNull(group.getName(), GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public ObjectNode createResponse(ContentPath contentPath, ObjectMapper mapper) {
        ObjectNode response = mapper.createObjectNode();
        response.put("name", group.getName());
        ArrayNode uris = response.putArray("uris");
        if (contentPath instanceof ContentKey) {
            uris.add(group.getChannelUrl() + "/" + contentPath.toUrl());
            response.put("type", "item");
        } else {
            response.put("id", contentPath.toUrl());
            response.put("type", "heartbeat");
        }
        return response; 
    }

    @Override
    public ContentPath inProcess(ContentPath contentPath) {
        return contentPath;
    }

    public Optional<ContentPath> next() {
        if (error.get()) {
            logger.error("unable to determine next");
        }
        try {
            return Optional.fromNullable(queue.poll(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void start(Group group, ContentPath startingPath) {
        ContentPath startingKey = (ContentPath) startingPath;
        channel = ChannelNameUtils.extractFromChannelUrl(group.getChannelUrl());
        queryGenerator = new QueryGenerator(startingKey.getTime(), channel);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("single-group-" + group.getName() + "-%s").build();
        executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {

            ContentPath lastAdded = startingKey;

            @Override
            public void run() {
                try {
                    while (!shouldExit.get()) {
                        if (!doWork()) {
                            Sleeper.sleep(1000);
                        }
                    }
                } catch (InterruptedException | RuntimeInterruptedException e) {
                    error.set(true);
                    logger.info("InterruptedException with " + channel);
                } catch (NoSuchChannelException e) {
                    error.set(true);
                    logger.debug("NoSuchChannelException for " + channel);
                } catch (Exception e) {
                    error.set(true);
                    logger.warn("unexpected issue with " + channel, e);
                }
            }

            private boolean doWork() throws InterruptedException {
                ActiveTraces.start("SingleGroupStrategy", group);
                try {
                    DateTime latestStableInChannel = TimeUtil.stable();
                    if (channelService.isReplicating(channel)) {
                        ContentPath contentPath = lastContentPath.get(channel, MinutePath.NONE, ChannelReplicator.REPLICATED_LAST_UPDATED);
                        latestStableInChannel = contentPath.getTime();
                    }
                    TimeQuery timeQuery = queryGenerator.getQuery(latestStableInChannel);
                    if (timeQuery != null) {
                        addKeys(channelService.queryByTime(timeQuery));
                        if (group.isHeartbeat() && queryGenerator.getLastQueryTime().getSecondOfMinute() == 0) {
                            MinutePath minutePath = new MinutePath(queryGenerator.getLastQueryTime().minusMinutes(1));
                            logger.debug("sending heartbeat {}", minutePath);
                            addKey(minutePath);
                        }
                        return true;
                    }
                    return false;
                } finally {
                    ActiveTraces.end();
                }
            }

            private void addKeys(Collection<ContentKey> keys) throws InterruptedException {
                logger.debug("channel {} keys {}", channel, keys);
                if (logger.isTraceEnabled()) {
                    ActiveTraces.getLocal().log(logger);
                }
                for (ContentKey key : keys) {
                    addKey(key);
                }
            }

            private void addKey(ContentPath key) throws InterruptedException {
                if (key.compareTo(lastAdded) > 0) {
                    queue.put(key);
                    lastAdded = key;
                }
            }

        });

    }


    @Override
    public void close() {
        GroupStrategy.close(shouldExit, executorService, queue);
    }
}
