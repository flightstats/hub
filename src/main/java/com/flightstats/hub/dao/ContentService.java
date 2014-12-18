package com.flightstats.hub.dao;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.google.common.base.Optional;

import java.util.Collection;

public interface ContentService {

    ContentKey insert(String channelName, Content content);

    Optional<Content> getValue(String channelName, ContentKey key);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    void delete(String channelName);

    Collection<ContentKey> getKeys(DirectionQuery query);
}
