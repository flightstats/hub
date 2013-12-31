package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ContentKey;

/**
 *
 */
public interface KeyCoordination {
    void insert(String channelName, ContentKey key);

    ContentKey getLastUpdated(String channelName);
}
