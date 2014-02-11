package com.flightstats.hub.replication;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChannelReplicator implements Runnable, Lockable {
    private static final Logger logger = LoggerFactory.getLogger(ChannelReplicator.class);

    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
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
                             CuratorLock curatorLock, SequenceIteratorFactory sequenceIteratorFactory) {
        this.channelService = channelService;
        this.curatorLock = curatorLock;
        this.sequenceIteratorFactory = sequenceIteratorFactory;
        this.channelUtils = channelUtils;
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
            curatorLock.runWithLock(this, "/ChannelReplicator/" + channel.getName(), 5, TimeUnit.SECONDS);
        } finally {
            Thread.currentThread().setName("Empty");
        }
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
        long sequence = getStartingSequence();
        if (sequence == ChannelUtils.NOT_FOUND) {
            return;
        }
        logger.info("starting " + channel.getUrl() + " migration at " + sequence);
        iterator = sequenceIteratorFactory.create(sequence, channel);
        while (iterator.hasNext() && curatorLock.shouldKeepWorking()) {
            channelService.insert(channel.getName(), iterator.next());
        }
    }

    long getStartingSequence() {
        Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channel.getName());
        if (lastUpdatedKey.isPresent()) {
            SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
            if (contentKey.getSequence() == SequenceContentKey.START_VALUE) {
                return searchForStartingKey(SequenceContentKey.START_VALUE, historicalDays);
            }
            return searchForStartingKey(contentKey.getSequence() + 1, historicalDays + 1);
        }
        logger.warn("problem getting starting sequence " + channel.getUrl());
        return ChannelUtils.NOT_FOUND;
    }

    long searchForStartingKey(long startValue, long daysToUse) {
        //this may not play well with discontinuous sequences
        logger.debug("searching the key space for " + channel.getUrl());
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channel.getUrl());
        if (!latestSequence.isPresent()) {
            return SequenceContentKey.START_VALUE + 1;
        }
        long high = latestSequence.get();
        long low = startValue;
        long lastExists = high;
        while (low <= high) {
            long middle = low + (high - low) / 2;
            if (existsAndNotYetExpired(middle, daysToUse)) {
                high = middle - 1;
                lastExists = middle;
            } else {
                low = middle + 1;
            }
        }
        logger.debug("returning starting key " + lastExists);
        return lastExists;
    }

    /**
     * We want to return a starting id that exists, and isn't going to be expired immediately.
     */
    private boolean existsAndNotYetExpired(long id, long daysToUse) {
        Optional<DateTime> creationDate = channelUtils.getCreationDate(channel.getUrl(), id);
        if (!creationDate.isPresent()) {
            return false;
        }
        //we can change to use ttlDays after we know there are no Hubs to migrate.
        long millis = Math.min(TimeUnit.DAYS.toMillis(daysToUse), configuration.getTtlMillis());
        DateTime tenMinuteOffset = new DateTime().minusMillis((int) millis);
        return creationDate.get().isAfter(tenMinuteOffset);
    }

    public boolean isConnected() {
        if (null == iterator) {
            return false;
        }
        return iterator.isConnected();
    }

}
