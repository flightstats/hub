package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;

public interface ChannelDao {

    boolean channelExists(String channelName);

    ChannelConfiguration createChannel(String name);

    ValueInsertionResult insert(String channelName, String contentType, byte[] data);

    LinkedDataHubCompositeValue getValue(String channelName, DataHubKey key);

    ChannelConfiguration getChannelConfiguration(String channelName);

    Optional<DataHubKey> findLatestId(String channelName);
}
