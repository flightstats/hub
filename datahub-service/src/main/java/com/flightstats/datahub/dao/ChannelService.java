package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.LinkedContent;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

public interface ChannelService {

	boolean channelExists(String channelName);

	ChannelConfiguration createChannel(ChannelConfiguration configuration);

	ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data);

	Optional<LinkedContent> getValue(String channelName, String id);

	ChannelConfiguration getChannelConfiguration(String channelName);

	Iterable<ChannelConfiguration> getChannels();

	Optional<ContentKey> findLastUpdatedKey(String channelName);

    boolean isHealthy();

	void updateChannel(ChannelConfiguration configuration);

    Iterable<ContentKey> getKeys(String channelName, DateTime dateTime);

    void delete(String channelName);
}
