package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

public interface ContentDao {

    String CACHE = "Cache";
    String SINGLE_LONG_TERM = "LongTerm";
    String BATCH_LONG_TERM = "BatchLongTerm";

    ContentKey insert(String channelName, Content content) throws Exception;

    default SortedSet<ContentKey> insert(BulkContent bulkContent) throws Exception {
        throw new UnsupportedOperationException("bulk writes are not supported");
    }

    Content get(String channelName, ContentKey key);

    SortedSet<ContentKey> queryByTime(TimeQuery timQuery);

    SortedSet<ContentKey> query(DirectionQuery query);

    void delete(String channelName);

    void initialize();

    Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces);

    void deleteBefore(String channelName, ContentKey limitKey);

    default void writeBatch(String channel, ContentPath path, Collection<ContentKey> keys, byte[] bytes) {
        throw new UnsupportedOperationException("writeBatch is not supported");
    }

    default boolean streamMinute(String channel, ContentPathKeys path, Consumer<Content> callback) {
        throw new UnsupportedOperationException("streamMinute is not supported");
    }

    default ContentKey insertHistorical(String channelName, Content content) throws Exception {
        throw new UnsupportedOperationException("insertHistorical is not supported");
    }

}
