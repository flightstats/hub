package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.Comparator;
import java.util.List;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.CHANNELS_LATEST_ROW_KEY;

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

	private Optional<DataHubKey> findFirstDifferentResult(DataHubKey inputKey, QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult, final boolean reversed) {
		OrderedRows<String, String, DataHubCompositeValue> rows = queryResult.get();

		// Because the row containing the latest channel item exists in the same column family with the same column name, we need to exclude it here.
		Iterable<Row<String, String, DataHubCompositeValue>> nonLatestRows = excludeLatestChannelItemRow(rows.getList());
		List<Row<String, String, DataHubCompositeValue>> sortedRows = getSortedRows(reversed, nonLatestRows);

		String inputKeyString = keyRenderer.keyToString(inputKey);
		for (Row<String, String, DataHubCompositeValue> row : sortedRows) {
			ColumnSlice<String, DataHubCompositeValue> columnSlice = row.getColumnSlice();
			Optional<DataHubKey> rowResult = findItemInRow(inputKeyString, columnSlice);
			if (rowResult.isPresent()) {
				return rowResult;
			}
		}
		return Optional.absent();
	}

	private Iterable<Row<String, String, DataHubCompositeValue>> excludeLatestChannelItemRow(List<Row<String, String, DataHubCompositeValue>> rows) {
		return Iterables.filter(rows,
				new Predicate<Row<String, String, DataHubCompositeValue>>() {
					@Override
					public boolean apply(Row<String, String, DataHubCompositeValue> input) {
						return !CHANNELS_LATEST_ROW_KEY.equals(input.getKey());
					}
				});
	}

	private List<Row<String, String, DataHubCompositeValue>> getSortedRows(final boolean reversed, Iterable<Row<String, String, DataHubCompositeValue>> rowsList) {
		return Ordering.from(new Comparator<Row<String, String, DataHubCompositeValue>>() {
			@Override
			public int compare(Row<String, String, DataHubCompositeValue> o1, Row<String, String, DataHubCompositeValue> o2) {
				String key1 = o1.getKey();
				String key2 = o2.getKey();
				return reversed ? key2.compareTo(key1) : key1.compareTo(key2);
			}
		}).sortedCopy(rowsList);
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
