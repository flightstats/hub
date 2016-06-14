package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

public interface ChannelService {
    boolean channelExists(String channelName);

    ChannelConfig createChannel(ChannelConfig configuration);

    ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig);

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(BulkContent bulkContent) throws Exception;

    boolean isReplicating(String channelName);

    Optional<ContentKey> getLatest(String channel, boolean stable, boolean trace);

    void deleteBefore(String name, ContentKey limitKey);

    Optional<Content> getValue(Request request);

    ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache);

    ChannelConfig getCachedChannelConfig(String channelName);

    Iterable<ChannelConfig> getChannels();

    Iterable<ChannelConfig> getChannels(String tag);

    Iterable<String> getTags();

    SortedSet<ContentKey> queryByTime(TimeQuery query);

    SortedSet<ContentKey> getKeys(DirectionQuery query);

    void getValues(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback);

    boolean delete(String channelName);
}
