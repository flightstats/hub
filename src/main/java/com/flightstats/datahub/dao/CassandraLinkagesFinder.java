package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;

public class CassandraLinkagesFinder {

    private final CassandraConnector connector;
    private final HectorFactoryWrapper hector;
    private final DataHubKeyRenderer keyRenderer;

    @Inject
    public CassandraLinkagesFinder(CassandraConnector connector, HectorFactoryWrapper hector, DataHubKeyRenderer keyRenderer) {
        this.connector = connector;
        this.hector = hector;
        this.keyRenderer = keyRenderer;
    }

    public Optional<DataHubKey> findPrevious(String channelName, DataHubKey key) {
        String inputKeyString = keyRenderer.keyToString(key);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = queryForPrevious(channelName, inputKeyString);
        OrderedRows<String, String, DataHubCompositeValue> rows = queryResult.get();
        for (Row<String, String, DataHubCompositeValue> row : rows) {
            ColumnSlice<String, DataHubCompositeValue> columnSlice = row.getColumnSlice();
            Optional<DataHubKey> rowResult = findItemInRow(inputKeyString, columnSlice);
            if (rowResult.isPresent()) {
                return rowResult;
            }
        }
        return Optional.absent();
    }

    private QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryForPrevious(String channelName, String maxKey) {
        Keyspace keyspace = connector.getKeyspace();
        String minKey = keyRenderer.keyToString(DataHubKey.MIN_KEY);
        return hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())
                     .setColumnFamily(channelName)
                     .setRange(maxKey, minKey, true, 2)
                     .execute();
    }

    /**
     * Attempts to find the first column in the row that doesn't have the input column name;
     */
    private Optional<DataHubKey> findItemInRow(String inputKeyString, ColumnSlice<String, DataHubCompositeValue> columnSlice) {
        for (HColumn<String, DataHubCompositeValue> column : columnSlice.getColumns()) {
            String columnName = column.getName();
            if (!columnName.equals(inputKeyString)) {
                return Optional.of(keyRenderer.fromString(columnName));
            }
        }
        return Optional.absent();
    }
}
