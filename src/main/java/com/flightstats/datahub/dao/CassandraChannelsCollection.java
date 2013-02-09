package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.Date;

public class CassandraChannelsCollection {

    static final String CHANNELS_ROW_KEY = "DATA_HUB_CHANNELS";
    static final String CHANNELS_COLUMN_FAMILY_NAME = "channelMetadata";

    private final CassandraConnector connector;
    private final Serializer<ChannelConfiguration> channelConfigSerializer;
    private final HectorFactoryWrapper hector;

    @Inject
    public CassandraChannelsCollection(CassandraConnector connector, Serializer<ChannelConfiguration> channelConfigSerializer, HectorFactoryWrapper hector) {
        this.connector = connector;
        this.channelConfigSerializer = channelConfigSerializer;
        this.hector = hector;
    }

    public ChannelConfiguration createChannel(String name, String description) {
        ChannelConfiguration channelConfig = new ChannelConfiguration(name, description, getDate());
        createColumnFamilyForChannel(channelConfig);
        addMetaDataForNewChannel(channelConfig);
        return channelConfig;
    }

    private void addMetaDataForNewChannel(ChannelConfiguration channelConfig) {
        connector.createColumnFamilyIfNeeded(CHANNELS_COLUMN_FAMILY_NAME);
        StringSerializer keySerializer = StringSerializer.get();
        Mutator<String> mutator = connector.buildMutator(keySerializer);
        //TODO: Guard with mutex?  Mutator is not a thread-safe class....
        HColumn<String, ChannelConfiguration> column = hector.createColumn(channelConfig.getName(), channelConfig, StringSerializer.get(),
                channelConfigSerializer);
        mutator.insert(CHANNELS_ROW_KEY, CHANNELS_COLUMN_FAMILY_NAME, column);
    }

    private void createColumnFamilyForChannel(ChannelConfiguration channelConfig) {
        String columnSpaceName = channelConfig.getName();
        connector.createColumnFamilyIfNeeded(columnSpaceName);
    }

    public boolean channelExists(String channelName) {
        ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
        return channelConfiguration != null;
    }

    private ChannelConfiguration getChannelConfiguration(String channelName) {
        connector.createColumnFamilyIfNeeded(CHANNELS_COLUMN_FAMILY_NAME);
        Keyspace keyspace = connector.getKeyspace();
        ColumnQuery<String, String, ChannelConfiguration> rawQuery = hector.createColumnQuery(keyspace, StringSerializer.get(),
                StringSerializer.get(), channelConfigSerializer);
        ColumnQuery<String, String, ChannelConfiguration> columnQuery = rawQuery.setName(channelName)
                                                                                .setKey(CHANNELS_ROW_KEY)
                                                                                .setColumnFamily(CHANNELS_COLUMN_FAMILY_NAME);
        QueryResult<HColumn<String, ChannelConfiguration>> result = columnQuery.execute();
        HColumn<String, ChannelConfiguration> column = result.get();
        return column == null ? null : column.getValue();
    }

    @VisibleForTesting
    Date getDate() {
        return new Date();
    }
}
