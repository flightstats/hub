package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;

public interface ContentService {

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(String channelName, BatchContent content) throws Exception;

    Optional<Content> getValue(String channelName, ContentKey key);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    void delete(String channelName);

    Collection<ContentKey> queryDirection(DirectionQuery query);

    Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces);

    void deleteBefore(String name, ContentKey limitKey);
}
