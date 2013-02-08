package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.Date;

import static me.prettyprint.hector.api.factory.HFactory.createColumnQuery;

public class CassandraChannelsCollection {

    private static final String CHANNELS_ROW_KEY = "DATA_HUB_CHANNELS";
    private static final String CHANNEL_COLUMN_FAMILY_NAME = "channelMetadata";

    private final CassandraConnector connector;
    private final Serializer<ChannelConfiguration> channelConfigSerializer;

    @Inject
    public CassandraChannelsCollection(CassandraConnector connector, Serializer<ChannelConfiguration> channelConfigSerializer) {
        this.connector = connector;
        this.channelConfigSerializer = channelConfigSerializer;
    }

    public ChannelConfiguration createChannel(String name, String description) {
        ChannelConfiguration channelConfig = new ChannelConfiguration(name, description, new Date());
        createColumnSpaceForChannel(channelConfig);
        addMetaDataForNewChannel(channelConfig);
        return channelConfig;
    }

    private void addMetaDataForNewChannel(ChannelConfiguration channelConfig) {
        connector.createColumnFamily(CHANNEL_COLUMN_FAMILY_NAME);
        StringSerializer keySerializer = StringSerializer.get();
        Mutator<String> mutator = connector.buildMutator(keySerializer);
        //TODO: Guard with mutex?  Mutator is not a thread-safe class....
        HColumn<String, ChannelConfiguration> column = HFactory.createColumn(channelConfig.getName(), channelConfig, StringSerializer.get(),
                channelConfigSerializer);
        mutator.insert(CHANNELS_ROW_KEY, CHANNEL_COLUMN_FAMILY_NAME, column);
    }

    private void createColumnSpaceForChannel(ChannelConfiguration channelConfig) {
        // Note: For now, these are intentionally the same thing, but we may wish to reevaluate this later.
        String key = channelConfig.getName();
        String columnSpaceName = channelConfig.getName();
        connector.createColumnFamily(columnSpaceName);
    }

    public boolean channelExists(String channelName) {
        ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
        return channelConfiguration != null;
    }

    public ChannelConfiguration getChannelConfiguration(String channelName) {
        Keyspace keyspace = connector.getKeyspace();

        connector.createColumnFamily(CHANNEL_COLUMN_FAMILY_NAME);

        ColumnQuery<String, String, ChannelConfiguration> columnQuery = createColumnQuery(keyspace, StringSerializer.get(),
                StringSerializer.get(), channelConfigSerializer).setName(channelName).setKey(CHANNELS_ROW_KEY).setColumnFamily(
                CHANNEL_COLUMN_FAMILY_NAME);
        QueryResult<HColumn<String, ChannelConfiguration>> result = columnQuery.execute();
        HColumn<String, ChannelConfiguration> column = result.get();
        return column == null ? null : column.getValue();
    }
}
