package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.ChannelNameUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SingleGroupStrategy implements GroupStrategy {

    private final static Logger logger = LoggerFactory.getLogger(SingleGroupStrategy.class);

    private final Group group;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private BlockingQueue<ContentPath> queue = new ArrayBlockingQueue<>(1000);
    private String channel;
    private QueryGenerator queryGenerator;


    public SingleGroupStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        this.group = group;
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
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
        return getLastCompleted(startingKey);
    }

    private ContentPath getLastCompleted(ContentPath defaultKey) {
        return lastContentPath.get(group.getName(), defaultKey, GroupLeader.GROUP_LAST_COMPLETED);
    }

    @Override
    public ContentPath getLastCompleted() {
        return getLastCompleted(ContentKey.NONE);
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
            throw new RuntimeException("unable to determine next");
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
        ExecutorService executorService = Executors.newSingleThreadExecutor(factory);
        executorService.submit(new Runnable() {

            ContentPath lastAdded = startingKey;

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
                            if (group.isHeartbeat() && queryGenerator.getLastQueryTime().getSecondOfMinute() == 0) {
                                MinutePath minutePath = new MinutePath(queryGenerator.getLastQueryTime().minusMinutes(1));
                                logger.debug("sending heartbeat {}", minutePath);
                                addKey(minutePath);
                            }
                        } else {
                            Sleeper.sleep(1000);
                        }
                    }
                }
            }

            private void handleReplication() {
                /**
                 * This does not currently send a minute heartbeat.
                 * Once the heartbeat is available in all envs, we can update this to use REPLICATED_LAST_UPDATED
                 */
                Collection<ContentKey> keys = Collections.EMPTY_LIST;
                Optional<ContentKey> latest = channelService.getLatest(channel, true, false);
                if (latest.isPresent() && latest.get().compareTo(lastAdded) > 0) {
                    DirectionQuery query = DirectionQuery.builder()
                            .channelName(channel)
                            .contentKey((ContentKey) lastAdded)
                            .next(true)
                            .stable(true)
                            .ttlDays(channelService.getCachedChannelConfig(channel).getTtlDays())
                            .count(1000)
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
                for (ContentKey key : keys) {
                    if (key.compareTo(lastAdded) > 0) {
                        addKey(key);
                        lastAdded = key;
                    }
                }
            }

            private void addKey(ContentPath key) {
                try {
                    queue.put(key);
                } catch (InterruptedException e) {
                    logger.info("InterruptedException " + channel + " " + e.getMessage());
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
