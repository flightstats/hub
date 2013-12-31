package com.flightstats.datahub.util;

import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.TimeSeriesContentKey;
import com.google.common.base.Optional;

/**
 *
 */
public class TimeSeriesKeyGenerator implements DataHubKeyGenerator {
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
}
