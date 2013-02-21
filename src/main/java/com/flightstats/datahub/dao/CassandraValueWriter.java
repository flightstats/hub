package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

public class CassandraValueWriter {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;
    private final DataHubKeyGenerator keyGenerator;
    private final DataHubKeyRenderer keyRenderer;

    @Inject
    public CassandraValueWriter(CassandraConnector connector, HectorFactoryWrapper hector, RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy, DataHubKeyGenerator keyGenerator, DataHubKeyRenderer keyRenderer) {
        this.connector = connector;
        this.hector = hector;
        this.rowKeyStrategy = rowKeyStrategy;
        this.keyGenerator = keyGenerator;
        this.keyRenderer = keyRenderer;
    }

    public ValueInsertionResult write(String channelName, DataHubCompositeValue columnValue) {
        Mutator<String> mutator = connector.buildMutator(StringSerializer.get());

        DataHubKey key = keyGenerator.newKey();

        String columnName = keyRenderer.keyToString(key);
        HColumn<String, DataHubCompositeValue> column = hector.createColumn(columnName, columnValue, StringSerializer.get(),
                DataHubCompositeValueSerializer.get());

        String rowKey = rowKeyStrategy.buildKey(channelName, key);

        mutator.insert(rowKey, channelName, column);
        return new ValueInsertionResult(key);
    }
}
