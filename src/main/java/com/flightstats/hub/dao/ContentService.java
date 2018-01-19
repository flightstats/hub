package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;

public interface ContentService {

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(BulkContent bulkContent) throws Exception;

    boolean historicalInsert(String channelName, Content content) throws Exception;

    Optional<Content> get(String channelName, ContentKey key, boolean cached, boolean skipLarge);

    void get(StreamResults streamResults);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    void delete(String channelName);

    void delete(String channelName, ContentKey contentKey);

    Collection<ContentKey> queryDirection(DirectionQuery query);

    Optional<ContentKey> getLatest(DirectionQuery query);

    default void deleteBefore(String name, ContentKey limitKey) {
        //do nothing
    }

    default void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        //do nothing
    }

    static Optional<ContentKey> chooseLatest(Collection<ContentKey> contentKeys, DirectionQuery query) {
        if (contentKeys.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(contentKeys.iterator().next());
    }
}
