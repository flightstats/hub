package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

public interface ChannelService {

	boolean channelExists(String channelName);

	ChannelConfiguration createChannel(ChannelConfiguration configuration);

	ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data);

	Optional<LinkedDataHubCompositeValue> getValue(String channelName, String id);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

	Optional<DataHubKey> findLastUpdatedKey(String channelName);

    boolean isHealthy();

	void updateChannelMetadata(ChannelConfiguration newConfig);

    Optional<Iterable<DataHubKey>> getKeys(String channelName, DateTime dateTime);
}
