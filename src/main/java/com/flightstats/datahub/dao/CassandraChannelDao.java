package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;

import java.util.Date;

public class CassandraChannelDao implements ChannelDao {

    @Override
    public boolean channelExists(String channelName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ChannelConfiguration createChannel(String name, String description) {
        Date date = new Date(); //now
        return new ChannelConfiguration(name, description, date);
    }
}
