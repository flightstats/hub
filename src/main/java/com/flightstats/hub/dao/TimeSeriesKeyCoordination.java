package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;

/**
 *
 */
public class TimeSeriesKeyCoordination implements KeyCoordination {
    @Override
    public void insert(String channelName, ContentKey key) {
        //does this need to do anything?
    }

    @Override
    public ContentKey getLastUpdated(String channelName) {
        throw new UnsupportedOperationException("last isn't supported " + channelName);
    }
}
