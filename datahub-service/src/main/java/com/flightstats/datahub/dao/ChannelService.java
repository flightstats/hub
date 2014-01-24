package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.Collection;

public interface ChannelService {

	boolean channelExists(String channelName);

	ChannelConfiguration createChannel(ChannelConfiguration configuration);

	ValueInsertionResult insert(String channelName, Content content);

	Optional<LinkedContent> getValue(String channelName, String id);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

	Optional<ContentKey> findLastUpdatedKey(String channelName);

    boolean isHealthy();

    ChannelConfiguration updateChannel(ChannelConfiguration configuration);

    Collection<ContentKey> getKeys(String channelName, DateTime dateTime);

    void delete(String channelName);
}
