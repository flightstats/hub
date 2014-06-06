package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;

public interface LastUpdatedDao {
    void update(String channelName, ContentKey key);

    ContentKey getLastUpdated(String channelName);

    void delete(String channelName);

    void initialize(String channelName);
}
