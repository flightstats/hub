package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface ChannelDao {

	boolean channelExists(String channelName);

	ChannelConfiguration createChannel(String channelName, Long ttl);

	/**
	 * Note, this operation is done within a front-end lock on the channel.  The implementation of this method
	 * can assume that it will be the only insert called for this channel during execution.
	 */
	ValueInsertionResult insert(String channelName, String contentType, byte[] data);

	Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

	Optional<DataHubKey> findFirstUpdateKey(String channelName);

	Optional<DataHubKey> findLastUpdatedKey(String channelName);

	int countChannels();

	void setFirstKey(String channelName, DataHubKey key);

	void deleteFirstKey(String channelName);

	void setLastUpdateKey(String channelName, DataHubKey key);

	void deleteLastUpdateKey(String channelName);

	/**
	 * Delete the keys and their corresponding values for the given channel.
	 */
	void delete(String channelName, List<DataHubKey> keys);

	Collection<DataHubKey> findKeysInRange(String channelName, Date startTime, Date endTime);
}
