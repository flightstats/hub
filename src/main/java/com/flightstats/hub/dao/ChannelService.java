package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;

public interface ChannelService {

    boolean channelExists(String channelName);

    ChannelConfig createChannel(ChannelConfig configuration);

    ContentKey insert(String channelName, Content content);

    Optional<Content> getValue(Request request);

    ChannelConfig getChannelConfig(String channelName);

    ChannelConfig getCachedChannelConfig(String channelName);

    Iterable<ChannelConfig> getChannels();

    Iterable<ChannelConfig> getChannels(String tag);

    Iterable<String> getTags();

    ChannelConfig updateChannel(ChannelConfig configuration);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    Collection<ContentKey> getKeys(DirectionQuery query);

    boolean delete(String channelName);

    boolean isReplicating(String channelName);

    Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace);

}
