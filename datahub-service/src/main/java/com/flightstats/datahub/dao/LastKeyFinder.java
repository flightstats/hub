package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;

public class LastKeyFinder {

    private final static Logger logger = LoggerFactory.getLogger(LastKeyFinder.class);

    private final HectorFactoryWrapper hector;
	private final CassandraChannelsCollection channelsCollection;
    private final DataHubKeyRenderer keyRenderer;
    private final CassandraConnector connector;
    private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;

    @Inject
    public LastKeyFinder(CassandraChannelsCollection channelsCollection, HectorFactoryWrapper hector,
                         DataHubKeyRenderer keyRenderer,
                         CassandraConnector connector,
                         RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy) {
        this.hector = hector;
        this.channelsCollection = channelsCollection;
        this.keyRenderer = keyRenderer;
        this.connector = connector;
        this.rowKeyStrategy = rowKeyStrategy;
    }

    /**
     * This presumes that the latest DataHubKey is not in the cache, and therefore the entire cluster
     * must have been stopped, losing state in Hazelcast.
     */
    public DataHubKey queryForLatestKey(String channelName) {
        String latestRowKey = channelsCollection.getLatestRowKey(channelName);
        if (latestRowKey == null) {
            logger.warn("unable to find latest row key, presuming channel has no data " + channelName);
            return null;
        }
        //todo - gfm - 11/11/13 - should this look at previous row keys?
        DataHubKey dataHubKey = queryForLatestInRow(latestRowKey);
        if (null == dataHubKey) {
            logger.warn("unable to find latest data hub key in row, presuming channel has no data " + channelName);
            return null;
        }
        return dataHubKey;
    }

    private DataHubKey queryForLatestInRow(String rowKey) {
        Keyspace keyspace = connector.getKeyspace();
        RangeSlicesQuery<String, String, DataHubCompositeValue> query = hector.createRangeSlicesQuery(
                keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get());
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results = query
                .setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)
                .setRange(null, null, true, 1)
                .setKeys(rowKey, rowKey)
                .execute();
        return findFirstInResults(results);
    }

    private DataHubKey findFirstInResults(QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results) {
        List<Row<String, String, DataHubCompositeValue>> orderedRows = results.get().getList();
        if (orderedRows.isEmpty()) {
            return null;
        }
        Row<String, String, DataHubCompositeValue> firstRow = orderedRows.get(0);
        List<HColumn<String, DataHubCompositeValue>> columns = firstRow.getColumnSlice().getColumns();
        if (columns.isEmpty()) {
            return null;
        }
        String columnName = columns.get(0).getName();
        return keyRenderer.fromString(columnName).get();
    }
}
