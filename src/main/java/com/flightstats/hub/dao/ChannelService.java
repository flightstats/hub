package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;

public interface ChannelService {

    boolean channelExists(String channelName);

    ChannelConfiguration createChannel(ChannelConfiguration configuration);

    ContentKey insert(String channelName, Content content);

    Optional<Content> getValue(Request request);

    ChannelConfiguration getChannelConfiguration(String channelName);

    Iterable<ChannelConfiguration> getChannels();

    Iterable<ChannelConfiguration> getChannels(String tag);

    Iterable<String> getTags();

    ChannelConfiguration updateChannel(ChannelConfiguration configuration);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery);

    Collection<ContentKey> getKeys(DirectionQuery query);

    boolean delete(String channelName);

    boolean isReplicating(String channelName);

    Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace);

    void delete(String channel, ContentKey key);
}
