package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentKey;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class V1ChannelReplicator implements Leader, ChannelReplicator {
    private static final Logger logger = LoggerFactory.getLogger(V1ChannelReplicator.class);
    public static final String V1_REPLICATE_LAST_COMPLETED = "/V1ReplicateLastCompleted/";

    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final SequenceFinder sequenceFinder;
    private final CuratorFramework curator;
    private final LastContentKey lastContentKey;
    private final SequenceIteratorFactory sequenceIteratorFactory;
    private ChannelConfiguration channel;

    private SequenceIterator iterator;
    private boolean valid = false;
    private String message = "";
    private CuratorLeader curatorLeader;
    public static int START_VALUE = 999;

    @Inject
    public V1ChannelReplicator(ChannelService channelService, ChannelUtils channelUtils,
                               SequenceIteratorFactory sequenceIteratorFactory,
                               SequenceFinder sequenceFinder, CuratorFramework curator,
                               LastContentKey lastContentKey) {
        this.channelService = channelService;
        this.sequenceIteratorFactory = sequenceIteratorFactory;
        this.channelUtils = channelUtils;
        this.sequenceFinder = sequenceFinder;
        this.curator = curator;
        this.lastContentKey = lastContentKey;
    }

    public void setChannel(ChannelConfiguration channel) {
        this.channel = channel;
    }

    public ChannelConfiguration getChannel() {
        return channel;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public boolean tryLeadership() {
        logger.debug("starting run " + channel);
        valid = validateRemoteChannel();
        if (!valid) {
            return false;
        }
        curatorLeader = new CuratorLeader(getLeaderPath(channel.getName()), this, curator);
        curatorLeader.start();
        return true;
    }

    private String getLeaderPath(String channelName) {
        return "/ChannelReplicator/" + channelName;
    }

    @Override
    public void takeLeadership(AtomicBoolean hasLeadership) {
        try {
            Thread.currentThread().setName("ChannelReplicator-" + channel.getName());
            logger.info("takeLeadership");
            valid = validateRemoteChannel();
            if (!valid) {
                exit();
                return;
            }
            replicate(hasLeadership);
        } finally {
            Thread.currentThread().setName("Empty");
        }
    }

    public void exit() {
        closeIterator();
        try {
            if (curatorLeader != null) {
                curatorLeader.close();
            }
        } catch (Exception e) {
            logger.warn("unable to close curatorLeader", e);
        }
    }

    private void closeIterator() {
        try {
            if (iterator != null) {
                iterator.exit();
            }
        } catch (Exception e) {
            logger.warn("unable to close iterator", e);
        }
    }

    @VisibleForTesting
    boolean validateRemoteChannel() {
        try {
            Optional<ChannelConfiguration> optionalConfig = channelUtils.getConfiguration(channel.getReplicationSource());
            if (!optionalConfig.isPresent()) {
                message = "remote channel missing for " + channel.getReplicationSource();
                logger.warn(message);
                return false;
            }
            logger.debug("configuration " + optionalConfig.get());
            return true;
        } catch (IOException e) {
            message = "IOException " + channel.getReplicationSource() + " " + e.getMessage();
            logger.warn(message);
            return false;
        }
    }

    private void replicate(AtomicBoolean hasLeadership) {
        long sequence = getLastUpdated();
        if (sequence == ChannelUtils.NOT_FOUND) {
            return;
        }
        logger.info("starting " + channel.getReplicationSource() + " migration at " + sequence);
        iterator = sequenceIteratorFactory.create(sequence, channel);
        try {
            while (iterator.hasNext() && hasLeadership.get()) {
                Optional<Content> optionalContent = iterator.next();
                if (optionalContent.isPresent()) {
                    Content content = optionalContent.get();
                    ContentKey nextKey = content.getContentKey().get();
                    ContentKey lastCompletedKey = lastContentKey.get(channel.getName(),
                            nextKey, V1_REPLICATE_LAST_COMPLETED);
                    if (nextKey.compareTo(lastCompletedKey) < 0) {
                        nextKey = new ContentKey(lastCompletedKey.getTime(), nextKey.getHash());
                        content.setContentKey(nextKey);
                    }
                    channelService.insert(channel.getName(), content);
                    lastContentKey.updateIncrease(nextKey, channel.getName(), V1_REPLICATE_LAST_COMPLETED);
                } else {
                    logger.warn("missing content for " + channel.getReplicationSource());
                }
            }
        } finally {
            logger.info("stopping " + channel.getReplicationSource() + " migration ");
            closeIterator();
        }
    }

    public long getLastUpdated() {
        DirectionQuery query = DirectionQuery.builder()
                .contentKey(new ContentKey())
                .ttlDays(0)
                .count(1)
                .channelName(channel.getName())
                .build();
        query.trace(false);
        Collection<ContentKey> keys = channelService.getKeys(query);
        if (!keys.isEmpty()) {
            ContentKey contentKey = keys.iterator().next();
            try {
                int sequence = Integer.parseInt(contentKey.getHash());
                return sequenceFinder.searchForLastUpdated(channel, sequence, 1, TimeUnit.DAYS);
            } catch (NumberFormatException e) {
                logger.warn("unable to parse existing content key {}", contentKey);
            }
        }
        return sequenceFinder.searchForLastUpdated(channel, START_VALUE, 0, TimeUnit.DAYS);
    }

    public boolean isConnected() {
        if (null == iterator) {
            return false;
        }
        return iterator.isConnected();
    }

}
