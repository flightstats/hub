package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;

import java.util.UUID;

public interface ChannelDao {

    boolean channelExists(String channelName);

    ChannelConfiguration createChannel(String name);

    UUID insert(String channelName, byte[] data);
}
