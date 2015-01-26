package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfiguration;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * SequenceFinder looks up the last updated sequence for a channel.
 */
public class SequenceFinder {
    private final static Logger logger = LoggerFactory.getLogger(SequenceFinder.class);

    private final ChannelUtils channelUtils;

    @Inject
    public SequenceFinder(ChannelUtils channelUtils) {
        this.channelUtils = channelUtils;
    }

    public long searchForLastUpdated(ChannelConfiguration channel, long lastUpdated, long time, TimeUnit timeUnit) {
        logger.debug("searching the key space with lastUpdated {}", lastUpdated);
        Optional<Long> latestSequence = channelUtils.getLatestV1(channel.getReplicationSource());
        if (!latestSequence.isPresent()) {
            return ChannelUtils.NOT_FOUND;
        }
        long high = latestSequence.get();
        long low = lastUpdated;
        long lastExists = high;
        while (low <= high) {
            long middle = (long) (low + (high - low) * .95);
            if (existsAndNotYetExpired(channel, middle, time, timeUnit)) {
                high = middle - 1;
                lastExists = middle;
            } else {
                low = middle + 1;
            }
        }
        lastExists -= 1;
        logger.info("returning lastExists {} {}", lastExists, channel);
        return lastExists;
    }

    /**
     * We want to return a starting id that exists, and isn't going to be expired immediately.
     */
    private boolean existsAndNotYetExpired(ChannelConfiguration channel, long id, long time, TimeUnit timeUnit) {
        logger.debug("id = {} time = {} {} ", id, time, timeUnit);
        Optional<DateTime> creationDate = channelUtils.getCreationDate(channel.getReplicationSource(), id);
        if (!creationDate.isPresent()) {
            return false;
        }
        long days = Math.min(timeUnit.toDays(time), channel.getTtlDays());
        return creationDate.get().isAfter(new DateTime().minusDays((int) days));
    }
}
