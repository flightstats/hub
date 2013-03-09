package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.List;

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

	public Optional<DataHubKey> findNext(String channelName, DataHubKey key) {
		return queryAndFindResult(channelName, key, DataHubKey.MAX_KEY, false);
	}

	public Optional<DataHubKey> findPrevious(String channelName, DataHubKey key) {
		return queryAndFindResult(channelName, key, DataHubKey.MIN_KEY, true);
	}

	private Optional<DataHubKey> queryAndFindResult(String channelName, DataHubKey key, DataHubKey maxKey, boolean reversed) {
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = queryRange(channelName, key, maxKey, reversed);
		return findFirstDifferentResult(key, queryResult, reversed);
	}

	private QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryRange(String channelName, DataHubKey key, DataHubKey maxKey, boolean reversed) {
		String start = keyRenderer.keyToString(key);
		String end = keyRenderer.keyToString(maxKey);
		Keyspace keyspace = connector.getKeyspace();

		return hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())
					 .setColumnFamily(channelName)
					 .setRange(start, end, reversed, 2)
					 .execute();
	}

	private Optional<DataHubKey> findFirstDifferentResult(DataHubKey inputKey, QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult, boolean reversed) {
		OrderedRows<String, String, DataHubCompositeValue> rows = queryResult.get();

		List<Row<String, String, DataHubCompositeValue>> rowsList = rows.getList();
		if (reversed) {
			rowsList = Lists.reverse(rowsList);
		}
		String inputKeyString = keyRenderer.keyToString(inputKey);
		for (Row<String, String, DataHubCompositeValue> row : rowsList) {
			ColumnSlice<String, DataHubCompositeValue> columnSlice = row.getColumnSlice();
			Optional<DataHubKey> rowResult = findItemInRow(inputKeyString, columnSlice);
			if (rowResult.isPresent()) {
				return rowResult;
			}
		}
		return Optional.absent();
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
