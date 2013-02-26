package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraChannelDao implements ChannelDao {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDao.class);
    private final CassandraChannelsCollection channelsCollection;
    private final CassandraValueWriter cassandraValueWriter;
    private final CassandraValueReader cassandraValueReader;
    private final CassandraLinkagesFinder linkagesFinder;

    @Inject
    public CassandraChannelDao(CassandraChannelsCollection channelsCollection, CassandraValueWriter cassandraValueWriter, CassandraValueReader cassandraValueReader, CassandraLinkagesFinder linkagesFinder) {
        this.channelsCollection = channelsCollection;
        this.cassandraValueWriter = cassandraValueWriter;
        this.cassandraValueReader = cassandraValueReader;
        this.linkagesFinder = linkagesFinder;
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
        logger.debug("Inserting " + data.length + " bytes of type " + contentType + " into channel " + channelName);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
        ValueInsertionResult result = cassandraValueWriter.write(channelName, value);
        channelsCollection.updateLastUpdatedKey(channelName, result.getKey());
        return result;
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        logger.debug("Fetching " + key.toString() + " from channel " + channelName);
        DataHubCompositeValue value = cassandraValueReader.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        Optional<DataHubKey> previous = linkagesFinder.findPrevious(channelName, key);
        return Optional.of(new LinkedDataHubCompositeValue(value, previous));
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return channelsCollection.getChannelConfiguration(channelName);

    }

    @Override
    public Optional<DataHubKey> findLatestId(String channelName) {
        return cassandraValueReader.findLatestId(channelName);
    }
}
