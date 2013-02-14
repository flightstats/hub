package com.flightstats.datahub.dao;

import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import java.util.UUID;

public class CassandraInserter {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final RowKeyStrategy<String, UUID, byte[]> rowKeyStrategy;

    @Inject
    public CassandraInserter(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, UUID, byte[]> rowKeyStrategy) {
        this.connector = connector;
        this.hector = hector;
        this.rowKeyStrategy = rowKeyStrategy;
    }

    public UUID insert(String channelName, byte[] data) {
        Mutator<String> mutator = connector.buildMutator(StringSerializer.get());

        UUID columnName = hector.getUniqueTimeUUIDinMillis();
        HColumn<UUID, byte[]> column = hector.createColumn(columnName, data, UUIDSerializer.get(), BytesArraySerializer.get());

        String rowKey = rowKeyStrategy.buildKey(channelName, columnName, data);

        mutator.insert(rowKey, channelName, column);
        return columnName;
    }
}
