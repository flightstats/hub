package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Consumer;

public interface ContentDao {

    String WRITE_CACHE = "WriteCache";
    String READ_CACHE = "ReadCache";
    String SINGLE_LONG_TERM = "LongTerm";
    String BATCH_LONG_TERM = "BatchLongTerm";
    String LARGE_PAYLOAD = "LargePayload";

    ContentKey insert(String channelName, Content content);

    default SortedSet<ContentKey> insert(BulkContent bulkContent) {
        throw new UnsupportedOperationException("bulk writes are not supported");
    }

    Content get(String channelName, ContentKey key);

    SortedSet<ContentKey> queryByTime(TimeQuery timQuery);

    SortedSet<ContentKey> query(DirectionQuery query);

    void delete(String channelName);

    default void initialize() {
        // do nothing
    };

    Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces);

    void deleteBefore(String channelName, ContentKey limitKey);

    default void writeBatch(String channel, ContentPath path, Collection<ContentKey> keys, byte[] bytes) {
        throw new UnsupportedOperationException("writeBatch is not supported");
    }

    default Map<ContentKey, Content> readBatch(String channelName, ContentKey key) throws IOException {
        throw new UnsupportedOperationException("readBatch is not supported");
    }

    default boolean streamMinute(String channel, MinutePath path, boolean descending, Consumer<Content> callback) {
        throw new UnsupportedOperationException("streamMinute is not supported");
    }

    default ContentKey insertHistorical(String channelName, Content content) throws Exception {
        throw new UnsupportedOperationException("insertHistorical is not supported");
    }

    default void delete(String channelName, ContentKey key) {
        throw new UnsupportedOperationException("delete key is not supported");
    }
}
