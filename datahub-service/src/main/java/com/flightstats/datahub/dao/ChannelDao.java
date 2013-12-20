package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

public interface ChannelDao {

	boolean channelExists(String channelName);

    //todo - gfm - 12/19/13 - use ChannelConfiguration instead
	ChannelConfiguration createChannel(String channelName, Long ttlMillis);

	ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data);

	Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key);

	Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

	Optional<DataHubKey> findLastUpdatedKey(String channelName);

    boolean isHealthy();

	void updateChannelMetadata(ChannelConfiguration newConfig);
}
