package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CassandraChannelDao implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);
    private final CassandraChannelsCollection channelsCollection;
    private final CassandraInserter cassandraInserter;

    @Inject
    public CassandraChannelDao(CassandraChannelsCollection channelsCollection, CassandraInserter cassandraInserter) {
        this.channelsCollection = channelsCollection;
        this.cassandraInserter = cassandraInserter;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsCollection.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String name) {
        logger.info("Creating channel name = " + name);
        return channelsCollection.createChannel(name);
    }

    @Override
    public UUID insert(String channelName, byte[] data) {
        logger.info("Inserting " + data.length + " bytes into channel " + channelName);
        return cassandraInserter.insert(channelName, data);
    }
}
