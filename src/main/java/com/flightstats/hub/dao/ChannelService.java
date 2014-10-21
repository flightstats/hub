package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.Collection;

public interface ChannelService {

	boolean channelExists(String channelName);

	ChannelConfiguration createChannel(ChannelConfiguration configuration);

	InsertedContentKey insert(String channelName, Content content);

    Optional<LinkedContent> getValue(Request request);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

    Iterable<ChannelConfiguration> getChannels(String tag);

    Iterable<String> getTags();

	Optional<ContentKey> findLastUpdatedKey(String channelName);

    ChannelConfiguration updateChannel(ChannelConfiguration configuration);

    Collection<ContentKey> getKeys(String channelName, DateTime dateTime);

    boolean delete(String channelName);
}
