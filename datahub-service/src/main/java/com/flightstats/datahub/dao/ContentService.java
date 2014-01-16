package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.Collection;

public interface ContentService {

	void createChannel(ChannelConfiguration configuration);

	void updateChannel(ChannelConfiguration configuration);

	ValueInsertionResult insert(ChannelConfiguration configuration, Content content);

	Optional<LinkedContent> getValue(String channelName, String id);

	Optional<ContentKey> findLastUpdatedKey(String channelName);

    Collection<ContentKey> getKeys(String channelName, DateTime dateTime);

    void delete(String channelName);
}
