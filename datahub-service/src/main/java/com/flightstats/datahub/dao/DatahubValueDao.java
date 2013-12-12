package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

import java.util.Collection;

/**
 *
 */
public interface DataHubValueDao {
    ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue, Optional<Integer> ttlSeconds);

    void delete(String channelName, Collection<DataHubKey> keys);

    DataHubCompositeValue read(String channelName, DataHubKey key);

    void initialize();

    void initializeChannel(String channelName);
}
