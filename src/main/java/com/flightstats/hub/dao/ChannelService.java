package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.SortedSet;

public interface ChannelService {

    boolean channelExists(String channelName);

    ChannelConfig createChannel(ChannelConfig configuration);

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(String channelName, BatchContent content) throws Exception;

    Optional<Content> getValue(Request request);

    ChannelConfig getChannelConfig(String channelName);

    ChannelConfig getCachedChannelConfig(String channelName);

    Iterable<ChannelConfig> getChannels();

    Iterable<ChannelConfig> getChannels(String tag);

    Iterable<String> getTags();

    ChannelConfig updateChannel(ChannelConfig configuration);

    SortedSet<ContentKey> queryByTime(TimeQuery timeQuery);

    SortedSet<ContentKey> getKeys(DirectionQuery query);

    boolean delete(String channelName);

    boolean isReplicating(String channelName);

    Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace);

    void deleteBefore(String name, ContentKey limitKey);
}
