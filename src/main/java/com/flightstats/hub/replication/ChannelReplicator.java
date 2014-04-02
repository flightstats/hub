package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChannelReplicator implements Runnable, Lockable {
    private static final Logger logger = LoggerFactory.getLogger(ChannelReplicator.class);

    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final SequenceFinder sequenceFinder;
    private final CuratorLock curatorLock;
    private final SequenceIteratorFactory sequenceIteratorFactory;
    private ChannelConfiguration configuration;

    private Channel channel;
    private SequenceIterator iterator;
    private long historicalDays;
    private boolean valid = false;
    private String message = "";

    @Inject
    public ChannelReplicator(ChannelService channelService, ChannelUtils channelUtils,
                             CuratorLock curatorLock, SequenceIteratorFactory sequenceIteratorFactory,
                             SequenceFinder sequenceFinder) {
        this.channelService = channelService;
        this.curatorLock = curatorLock;
        this.sequenceIteratorFactory = sequenceIteratorFactory;
        this.channelUtils = channelUtils;
        this.sequenceFinder = sequenceFinder;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setHistoricalDays(long historicalDays) {
        this.historicalDays = historicalDays;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void run() {
        try {
            logger.debug("starting run " + channel);
            Thread.currentThread().setName("ChannelReplicator-" + channel.getUrl());
            valid = verifyRemoteChannel();
            if (!valid) {
                return;
            }
            curatorLock.runWithLock(this, getLockPath(channel.getName()), 5, TimeUnit.SECONDS);
        } finally {
            Thread.currentThread().setName("Empty");
        }
    }

    private String getLockPath(String channelName) {
        return "/ChannelReplicator/" + channelName;
    }

    @Override
    public void runWithLock() throws IOException {
        logger.info("run with lock " + channel.getUrl());
        initialize();
        replicate();
    }

    public void exit() {
        if (iterator != null) {
            iterator.exit();
        }
    }

    public void delete(String channelName) {
        curatorLock.delete(getLockPath(channelName));
    }

    @VisibleForTesting
    boolean verifyRemoteChannel() {
        try {
            Optional<ChannelConfiguration> optionalConfig = channelUtils.getConfiguration(channel.getUrl());
            if (!optionalConfig.isPresent()) {
                message = "remote channel missing for " + channel.getUrl();
                logger.warn(message);
                return false;
            }
            configuration = optionalConfig.get();
            channel.setConfiguration(configuration);
            logger.debug("configuration " + configuration);
            if (!configuration.isSequence()) {
                message = "Non-Sequence channels are not currently supported " + channel.getUrl();
                logger.warn(message);
                return false;
            }
            return true;
        } catch (IOException e) {
            message = "IOException " + channel.getUrl() + " " + e.getMessage();
            logger.warn(message);
            return false;
        }
    }

    @VisibleForTesting
    void initialize() throws IOException {
        if (!channelService.channelExists(configuration.getName())) {
            logger.info("creating channel for " + channel.getUrl());
            channelService.createChannel(configuration);
        }
    }

    private void replicate() {
        long sequence = sequenceFinder.getLastUpdated(channel, historicalDays);
        if (sequence == ChannelUtils.NOT_FOUND) {
            return;
        }
        logger.info("starting " + channel.getUrl() + " migration at " + sequence);
        iterator = sequenceIteratorFactory.create(sequence, channel);
        while (iterator.hasNext() && curatorLock.shouldKeepWorking()) {
            Optional<Content> optionalContent = iterator.next();
            if (optionalContent.isPresent()) {
                channelService.insert(channel.getName(), optionalContent.get());
            } else {
                logger.warn("missing content for " + channel.getUrl());
            }
        }
    }

    public boolean isConnected() {
        if (null == iterator) {
            return false;
        }
        return iterator.isConnected();
    }

}
