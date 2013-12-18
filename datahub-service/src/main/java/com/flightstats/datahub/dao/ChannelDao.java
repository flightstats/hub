package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

import java.util.List;

public interface ChannelDao {

	boolean channelExists(String channelName);

	ChannelConfiguration createChannel(String channelName, Long ttlMillis);

	ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data);

	Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

	Optional<DataHubKey> findLastUpdatedKey(String channelName);

    boolean isHealthy();

	void setLastUpdateKey(String channelName, DataHubKey key);

	/**
	 * Delete the keys and their corresponding values for the given channel.
	 */
	void delete(String channelName, List<DataHubKey> keys);

	void updateChannelMetadata(ChannelConfiguration newConfig);
}
