package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.Collection;

public interface ContentService {

    void createChannel(ChannelConfiguration configuration);

    InsertedContentKey insert(ChannelConfiguration configuration, Content content);

    Optional<Content> getValue(String channelName, String id);

    Optional<ContentKey> findLastUpdatedKey(String channelName);

    Collection<ContentKey> getKeys(String channelName, DateTime startTime, DateTime endTime);

    void delete(String channelName);

    Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count);
}
