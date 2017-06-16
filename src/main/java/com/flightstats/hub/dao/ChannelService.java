package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

public interface ChannelService {
    String DELEGATE = "DELEGATE";

    boolean channelExists(String channelName);

    ChannelConfig createChannel(ChannelConfig configuration);

    ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig, boolean isLocalHost);

    ContentKey insert(String channelName, Content content) throws Exception;

    boolean historicalInsert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(BulkContent bulkContent) throws Exception;

    boolean isReplicating(String channelName);

    /**
     * Latest exists as a separate path than query(DirectionQuery) to allow the underlying
     * storage system to highly optimize this frequent and potentially expensive operation.
     */
    Optional<ContentKey> getLatest(DirectionQuery query);

    default Optional<ContentKey> getLatest(String channel, boolean stable) {
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .next(false)
                .stable(stable)
                .count(1)
                .build();
        return getLatest(query);
    }

    void deleteBefore(String channel, ContentKey limitKey);

    Optional<Content> get(ItemRequest itemRequest);

    void get(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback);

    ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache);

    ChannelConfig getCachedChannelConfig(String channelName);

    Collection<ChannelConfig> getChannels();

    Collection<ChannelConfig> getChannels(String tag, boolean useCache);

    Iterable<String> getTags();

    SortedSet<ContentKey> queryByTime(TimeQuery query);

    SortedSet<ContentKey> query(DirectionQuery query);

    boolean delete(String channelName);

    boolean delete(String channelName, ContentKey contentKey);

    ContentPath getLastUpdated(String channelName, ContentPath defaultValue);
}
