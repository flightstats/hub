package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

/**
 *
 */
public interface ContentDao {
    ValueInsertionResult write(String channelName, Content columnValue, Optional<Integer> ttlSeconds);

    Content read(String channelName, ContentKey key);

    void initialize();

    void initializeChannel(ChannelConfiguration configuration);

    Optional<ContentKey> getKey(String id);

    Iterable<ContentKey> getKeys(String channelName, DateTime dateTime);
}
