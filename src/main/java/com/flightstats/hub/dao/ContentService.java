package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

public interface ContentService {

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(BulkContent bulkContent) throws Exception;

    Optional<Content> getValue(String channelName, ContentKey key);

    void getValues(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    void delete(String channelName);

    Collection<ContentKey> queryDirection(DirectionQuery query);

    Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces, boolean stable);

    void deleteBefore(String name, ContentKey limitKey);

    default void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        //do nothing
    }
}
