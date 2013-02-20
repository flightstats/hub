package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ValueInsertionResult;

import java.util.UUID;

public interface ChannelDao {

    boolean channelExists(String channelName);

    ChannelConfiguration createChannel(String name);

    ValueInsertionResult insert(String channelName, byte[] data);

    byte[] getValue(String channelName, UUID id);
}
