package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import org.joda.time.DateTime;

import java.util.Collection;

public interface ContentDao {

    InsertedContentKey write(String channelName, Content content);

    Content read(String channelName, ContentKey key);

    void initializeChannel(ChannelConfiguration configuration);

    Collection<ContentKey> getKeys(String channelName, DateTime startTime, DateTime endTime);

    Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count);

    void delete(String channelName);

    //todo - gfm - 11/12/14 - delete for cache/ttl?
}
