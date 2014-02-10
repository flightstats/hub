package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.Collection;

public interface ContentService {

	void createChannel(ChannelConfiguration configuration);

	void updateChannel(ChannelConfiguration configuration);

	InsertedContentKey insert(ChannelConfiguration configuration, Content content);

	Optional<LinkedContent> getValue(String channelName, String id);

	Optional<ContentKey> findLastUpdatedKey(String channelName);

    Collection<ContentKey> getKeys(String channelName, DateTime dateTime);

    void delete(String channelName);
}
