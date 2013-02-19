package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;

import java.util.UUID;

public interface ChannelDao {

    boolean channelExists(String channelName);

    ChannelConfiguration createChannel(String name);

    ValueInsertionResult insert(String channelName, String contentType, byte[] data);

    DataHubCompositeValue getValue(String channelName, UUID id);
}
