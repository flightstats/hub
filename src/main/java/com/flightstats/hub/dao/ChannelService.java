package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

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

    Collection<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit);

    Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count);

    boolean delete(String channelName);
}
