package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;

/**
 *
 */
public interface KeyCoordination {
    void insert(String channelName, ContentKey key);

    ContentKey getLastUpdated(String channelName);

    void delete(String channelName);
}
