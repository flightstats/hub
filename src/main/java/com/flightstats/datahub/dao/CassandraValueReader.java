package com.flightstats.datahub.dao;

import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.UUID;

public class CassandraValueReader {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final RowKeyStrategy<String, UUID, byte[]> rowKeyStrategy;

    @Inject
    public CassandraValueReader(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, UUID, byte[]> rowKeyStrategy) {
        this.connector = connector;
        this.hector = hector;
        this.rowKeyStrategy = rowKeyStrategy;
    }

    public byte[] read(String channelName, UUID id) {
        Keyspace keyspace = connector.getKeyspace();
        ColumnQuery<String, UUID, byte[]> query = hector.createColumnQuery(keyspace, StringSerializer.get(), UUIDSerializer.get(),
                BytesArraySerializer.get());
        String key = rowKeyStrategy.buildKey(channelName, id);
        QueryResult<HColumn<UUID, byte[]>> queryResult = query.setColumnFamily(channelName)
                                                              .setKey(key)
                                                              .setName(id)
                                                              .execute();
        HColumn<UUID, byte[]> column = queryResult.get();
        return column == null ? null : column.getValue();
    }
}
