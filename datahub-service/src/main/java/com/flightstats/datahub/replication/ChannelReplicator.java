package com.flightstats.datahub.replication;

import com.flightstats.datahub.cluster.CuratorLock;
import com.flightstats.datahub.cluster.Lockable;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.eventing.ChannelNameExtractor;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChannelReplicator implements Runnable, Lockable {
    //todo - gfm - 1/27/14 - this should push stats into graphite - lag time and number
    private static final Logger logger = LoggerFactory.getLogger(ChannelReplicator.class);

    private final ChannelService channelService;
    private final ChannelUtils channelUtils;
    private final CuratorLock curatorLock;
    private final SequenceIteratorFactory sequenceIteratorFactory;
    private ChannelConfiguration configuration;

    private String channelUrl;
    private String channel;
    private SequenceIterator iterator;
    private long historicalDays;

    @Inject
    public ChannelReplicator(ChannelService channelService, ChannelUtils channelUtils,
                             CuratorLock curatorLock, SequenceIteratorFactory sequenceIteratorFactory) {
        this.channelService = channelService;
        this.curatorLock = curatorLock;
        this.sequenceIteratorFactory = sequenceIteratorFactory;
        this.channelUtils = channelUtils;
    }

    public void setChannelUrl(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += "/";
        }
        this.channelUrl = channelUrl;
        this.channel = ChannelNameExtractor.extractFromChannelUrl(channelUrl);
    }

    public void setHistoricalDays(long historicalDays) {
        this.historicalDays = historicalDays;
    }

    public String getChannelName() {
        return channel;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    @Override
    public void run() {
        logger.info("starting run " + channelUrl);
        Thread.currentThread().setName("ChannelReplicator" + channelUrl);
        //todo - gfm - 1/29/14 - not sure why this is taking a minute to finally acquire the lock on local machine
        curatorLock.runWithLock(this, "/ChannelReplicator/" + channel, 5, TimeUnit.SECONDS);
        Thread.currentThread().setName("EmptyChannelReplicator");
    }

    @Override
    public void runWithLock() throws IOException {
        logger.info("run with lock " + channelUrl);
        if (!initialize())  {
            return;
        }
        replicate();
    }

    public void exit() {
        if (iterator != null) {
            iterator.exit();
        }
    }

    boolean initialize() throws IOException {
        Optional<ChannelConfiguration> optionalConfig = channelUtils.getConfiguration(channelUrl);
        if (!optionalConfig.isPresent()) {
            logger.warn("remote channel missing for " + channelUrl);
            return false;
        }
        configuration = optionalConfig.get();
        if (!configuration.isSequence()) {
            logger.warn("Non-Sequence channels are not currently supported " + channelUrl);
            return false;
        }
        //todo - gfm - 1/20/14 - this should verify the config hasn't changed
        if (!channelService.channelExists(configuration.getName())) {
            logger.info("creating channel for " + channelUrl);
            channelService.createChannel(configuration);
        }
        return true;
    }

    private void replicate() {
        long sequence = getStartingSequence();
        if (sequence == ChannelUtils.NOT_FOUND) {
            return;
        }
        logger.info("starting " + channelUrl + " migration at " + sequence);
        iterator = sequenceIteratorFactory.create(sequence, channelUrl);
        while (iterator.hasNext() && curatorLock.shouldKeepWorking()) {
            channelService.insert(channel, iterator.next());
        }
    }

    long getStartingSequence() {
        Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channel);
        if (lastUpdatedKey.isPresent()) {
            SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
            if (contentKey.getSequence() == SequenceContentKey.START_VALUE) {
                return searchForStartingKey();
            }
            return contentKey.getSequence() + 1;
        }
        logger.warn("problem getting starting sequence " + channelUrl);
        return ChannelUtils.NOT_FOUND;
    }

    long searchForStartingKey() {
        //this may not play well with discontinuous sequences
        logger.debug("searching the key space for " + channelUrl);
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
        if (!latestSequence.isPresent()) {
            return SequenceContentKey.START_VALUE + 1;
        }
        long high = latestSequence.get();
        long low = SequenceContentKey.START_VALUE;
        long lastExists = high;
        while (low <= high && (high - low) > 1) {
            long middle = low + (high - low) / 2;
            if (existsAndNotYetExpired(middle)) {
                high = middle - 1;
                lastExists = middle;
            } else {
                low = middle;
            }
        }
        logger.debug("returning starting key " + lastExists);
        return lastExists;
    }

    /**
     * We want to return a starting id that exists, and isn't going to be expired immediately.
     */
    private boolean existsAndNotYetExpired(long id) {
        Optional<DateTime> creationDate = channelUtils.getCreationDate(channelUrl, id);
        if (!creationDate.isPresent()) {
            return false;
        }
        //can change this when migration goes away.
        if (configuration.getTtlMillis() == null) {
            return true;
        }
        long millis = Math.min(TimeUnit.DAYS.toMillis(historicalDays), configuration.getTtlMillis());
        DateTime tenMinuteOffset = new DateTime().minusMillis((int) millis).plusMinutes(10);
        return creationDate.get().isAfter(tenMinuteOffset);
    }

}
