package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

/**
 *
 */
public interface DataHubValueDao {
    ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue, Optional<Integer> ttlSeconds);

    DataHubCompositeValue read(String channelName, DataHubKey key);

    void initialize();

    void initializeChannel(ChannelConfiguration configuration);

    Optional<DataHubKey> getKey(String id);
}
