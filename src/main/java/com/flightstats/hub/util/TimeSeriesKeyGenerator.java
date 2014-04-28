package com.flightstats.hub.util;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.TimeSeriesContentKey;
import com.google.common.base.Optional;

/**
 *
 */
public class TimeSeriesKeyGenerator implements ContentKeyGenerator {
    @Override
    public TimeSeriesContentKey newKey(String channelName) {
        return new TimeSeriesContentKey();
    }

    @Override
    public void seedChannel(String channelName) {
        //do nothing
    }

    @Override
    public Optional<ContentKey> parse(String keyString) {
        return TimeSeriesContentKey.fromString(keyString);
    }

    @Override
    public void delete(String channelName) {
        //do nothing
    }

    @Override
    public void setLatest(String channelName, ContentKey contentKey) {
        //do nothing
    }
}
