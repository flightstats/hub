package com.flightstats.hub.dao;

import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.StreamResults;
import com.flightstats.hub.model.TimeQuery;

import java.util.Collection;
import java.util.Optional;

public interface ContentService {

    static Optional<ContentKey> chooseLatest(Collection<ContentKey> contentKeys) {
        if (contentKeys.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(contentKeys.iterator().next());
    }

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(BulkContent bulkContent) throws Exception;

    boolean historicalInsert(String channelName, Content content) throws Exception;

    Optional<Content> get(String channelName, ContentKey key, boolean cached);

    void get(StreamResults streamResults);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    void delete(String channelName);

    void delete(String channelName, ContentKey contentKey);

    Collection<ContentKey> queryDirection(DirectionQuery query);

    Optional<ContentKey> getLatest(DirectionQuery query);

    default void deleteBefore(String name, ContentKey limitKey) {
        //do nothing
    }

    default void deleteBefore(String name, ContentKey limitKey, String bucketName) {
        //do nothing
    }

    default void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        //do nothing
    }
}
