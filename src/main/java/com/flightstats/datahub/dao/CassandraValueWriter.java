package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import java.util.Date;
import java.util.UUID;

public class CassandraValueWriter {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final RowKeyStrategy<String, UUID, DataHubCompositeValue> rowKeyStrategy;

    @Inject
    public CassandraValueWriter(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, UUID, DataHubCompositeValue> rowKeyStrategy) {
        this.connector = connector;
        this.hector = hector;
        this.rowKeyStrategy = rowKeyStrategy;
    }

    public ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue) {
        Mutator<String> mutator = connector.buildMutator(StringSerializer.get());

        UUID columnName = hector.getUniqueTimeUUIDinMillis();
        HColumn<UUID, DataHubCompositeValue> column = hector.createColumn(columnName, columnValue, UUIDSerializer.get(),
                DataHubCompositeValueSerializer
                        .get());

        String rowKey = rowKeyStrategy.buildKey(channelName, columnName);

        mutator.insert(rowKey, channelName, column);
        Date date = hector.getDateFromUUID(columnName);
        return new ValueInsertionResult(columnName, date);
    }
}
