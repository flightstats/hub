package com.flightstats.hub.replication;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
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

    private final ChannelService channelService;
    private final ChannelUtils channelUtils;

    @Inject
    public SequenceFinder(ChannelService channelService, ChannelUtils channelUtils) {
        this.channelService = channelService;
        this.channelUtils = channelUtils;
    }

    public long getLastUpdated(Channel channel, long historicalDays) {
        Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channel.getName());
        if (lastUpdatedKey.isPresent()) {
            SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
            if (contentKey.getSequence() == SequenceContentKey.START_VALUE) {
                return searchForLastUpdated(channel, SequenceContentKey.START_VALUE, historicalDays);
            }
            return searchForLastUpdated(channel, contentKey.getSequence(), historicalDays + 1);
        }
        logger.warn("problem getting starting sequence " + channel.getUrl());
        return ChannelUtils.NOT_FOUND;
    }

    long searchForLastUpdated(Channel channel, long lastUpdated, long historicalDays) {
        //this may not play well with discontinuous sequences
        logger.debug("searching the key space with lastUpdated {}", lastUpdated);
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channel.getUrl());
        if (!latestSequence.isPresent()) {
            return SequenceContentKey.START_VALUE;
        }
        long high = latestSequence.get();
        long low = lastUpdated;
        long lastExists = high;
        while (low <= high) {
            long middle = low + (high - low) / 2;
            if (existsAndNotYetExpired(channel, middle, historicalDays)) {
                high = middle - 1;
                lastExists = middle;
            } else {
                low = middle + 1;
            }
        }
        lastExists -= 1;
        logger.debug("returning lastExists {} ", lastExists);
        return lastExists;
    }

    /**
     * We want to return a starting id that exists, and isn't going to be expired immediately.
     */
    private boolean existsAndNotYetExpired(Channel channel, long id, long historicalDays) {
        logger.debug("id = {} daysToUse = {} ", id,  historicalDays);
        Optional<DateTime> creationDate = channelUtils.getCreationDate(channel.getUrl(), id);
        if (!creationDate.isPresent()) {
            return false;
        }
        //we can change to use ttlDays after we know there are no Hubs to migrate.
        long millis = Math.min(TimeUnit.DAYS.toMillis(historicalDays), channel.getConfiguration().getTtlMillis());
        DateTime tenMinuteOffset = new DateTime().minusMillis((int) millis);
        return creationDate.get().isAfter(tenMinuteOffset);
    }
}
