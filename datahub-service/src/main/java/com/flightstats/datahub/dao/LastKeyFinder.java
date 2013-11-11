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

import java.util.Date;
import java.util.List;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;

public class LastKeyFinder {

	private final HectorFactoryWrapper hector;
	private final CassandraChannelsCollection channelsCollection;
	private final DataHubKeyRenderer keyRenderer;
	private final CassandraConnector connector;
	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;

	@Inject
	public LastKeyFinder(CassandraChannelsCollection channelsCollection, HectorFactoryWrapper hector, DataHubKeyRenderer keyRenderer, CassandraConnector connector, RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy) {
		this.hector = hector;
		this.channelsCollection = channelsCollection;
		this.keyRenderer = keyRenderer;
		this.connector = connector;
		this.rowKeyStrategy = rowKeyStrategy;
	}

	public DataHubKey queryForLatestKey(String channelName) {
		DataHubKey firstKey = channelsCollection.getFirstKey(channelName);
		if (firstKey == null) {
			return null;
		}
		String firstRowKey = rowKeyStrategy.buildKey(channelName, firstKey);
		return searchBackToFirst(channelName, firstRowKey);
	}

	// Hunt backwards through the rowkeys, stopping on the first row that contains a column
	private DataHubKey searchBackToFirst(String channelName, String firstRowKey) {
		String rowBeingChecked = buildRowKeyAfterNow(channelName);
		while (rowIsNotBeforeFirst(rowBeingChecked, firstRowKey)) {
			DataHubKey latestKey = queryForLatestInRow(rowBeingChecked);
			if (latestKey != null) {
				return latestKey;
			}
			rowBeingChecked = rowKeyStrategy.prevKey(channelName, rowBeingChecked);
		}
		return null;
	}

	private boolean rowIsNotBeforeFirst(String rowBeingChecked, String firstRowKey) {
		return rowBeingChecked.compareTo(firstRowKey) >= 0;
	}

	private DataHubKey queryForLatestInRow(String rowKey) {
		Keyspace keyspace = connector.getKeyspace();
		RangeSlicesQuery<String, String, DataHubCompositeValue> query = hector.createRangeSlicesQuery(
				keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get());
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> results = query.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)
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

	private String buildRowKeyAfterNow(String channelName) {
		String currentKey = rowKeyStrategy.buildKey(channelName, new DataHubKey((short) 0));
		return rowKeyStrategy.nextKey(channelName, currentKey);
	}
}
