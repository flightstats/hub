package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;

public interface ChannelDao {

    boolean channelExists(String channelName);

    ChannelConfiguration createChannel(String name);
}
