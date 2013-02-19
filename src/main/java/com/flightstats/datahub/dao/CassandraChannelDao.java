package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CassandraChannelDao implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);
    private final CassandraChannelsCollection channelsCollection;
    private final CassandraValueWriter cassandraValueWriter;
    private final CassandraValueReader cassandraValueReader;

    @Inject
    public CassandraChannelDao(CassandraChannelsCollection channelsCollection, CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader) {
        this.channelsCollection = channelsCollection;
        this.cassandraValueWriter = cassandraValueWriter;
        this.cassandraValueReader = cassandraValueReader;
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
    public ValueInsertionResult insert(String channelName, String contentType, byte[] data) {
        logger.info("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
        return cassandraValueWriter.write(channelName, value);
    }

    @Override
    public DataHubCompositeValue getValue(String channelName, UUID id) {
        logger.info("Fetching " + id.toString() + " from channel " + channelName);
        return cassandraValueReader.read(channelName, id);
    }
}
