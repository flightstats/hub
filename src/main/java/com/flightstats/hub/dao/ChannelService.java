package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.TimeQuery;
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

    Optional<ContentKey> findLastUpdatedKey(String channelName);

    ChannelConfiguration updateChannel(ChannelConfiguration configuration);

    Collection<ContentKey> queryByTime(TimeQuery timeQuery, boolean stable);

    Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count);

    boolean delete(String channelName);
}
