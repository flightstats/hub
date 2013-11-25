package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If we switch to a persistent store for keys, this can go away
 */
public class LastKeyFinder {

    private final static Logger logger = LoggerFactory.getLogger(LastKeyFinder.class);

	private final CassandraChannelsCollection channelsCollection;
    private final DataHubKeyRenderer keyRenderer;

    @Inject
    public LastKeyFinder(CassandraChannelsCollection channelsCollection,
                         DataHubKeyRenderer keyRenderer) {
        this.channelsCollection = channelsCollection;
        this.keyRenderer = keyRenderer;
    }

    /**
     * This presumes that the latest DataHubKey is not in the cache, and therefore the entire cluster
     * must have been stopped, losing state in Hazelcast.
     * todo - gfm - 11/25/13 - this currently isn't implemented in cql, may go away, either by:
     * 1 - switching to Zookeeper for sequence generation
     * 2 - by querying for latest values with cql.
     */
    public DataHubKey queryForLatestKey(String channelName) {
        /*String latestRowKey = channelsCollection.getLatestRowKey(channelName);
        if (latestRowKey == null) {
            logger.warn("unable to find latest row key, presuming channel has no data " + channelName);
            return null;
        }
        DataHubKey dataHubKey = queryForLatestInRow(latestRowKey);
        if (null == dataHubKey) {
            logger.warn("unable to find latest data hub key in row, presuming channel has no data " + channelName);
            return null;
        }
        return dataHubKey;*/
        return null;
    }

    /*private DataHubKey queryForLatestInRow(String rowKey) {
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
    }*/
}
