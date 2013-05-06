package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.CHANNELS_LATEST_ROW_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraLinkagesFinderTest {

	private String channelName;
	private DataHubKeyRenderer keyRenderer;
	private DataHubKey targetKey;
	private DataHubKey expectedPrevious;
	private DataHubKey expectedNext;
	private CassandraConnector connector;
	private HectorFactoryWrapper hector;
	private Keyspace keyspace;
	private RangeSlicesQuery<String, String, DataHubCompositeValue> rangeQuery;
	private QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult;
	private OrderedRows orderedRows;
	private HColumn<String, DataHubCompositeValue> column;
	private HColumn<String, DataHubCompositeValue> previousColumn;
	private HColumn<String, DataHubCompositeValue> nextColumn;
	private Row row;
	private Row row1;
	private Row row2;
	private Row latestItemRow;
	private ColumnSlice columnSlice;
	private ColumnSlice columnSlice1;
	private ColumnSlice columnSlice2;

	@Before
	public void setup() {
		channelName = "shimsham";
		keyRenderer = new DataHubKeyRenderer();
		targetKey = new DataHubKey(new Date(555L), (short) 1);
		expectedPrevious = new DataHubKey(new Date(554L), (short) 0);
		expectedNext = new DataHubKey(new Date(556L), (short) 0);

		connector = mock(CassandraConnector.class);
		hector = mock(HectorFactoryWrapper.class);
		keyspace = mock(Keyspace.class);
		rangeQuery = mock(RangeSlicesQuery.class);
		queryResult = mock(QueryResult.class);
		orderedRows = mock(OrderedRows.class);
		column = mock(HColumn.class);
		previousColumn = mock(HColumn.class);
		nextColumn = mock(HColumn.class);
		row = mock(Row.class);
		latestItemRow = mock(Row.class);

		row1 = mock(Row.class);
		row2 = mock(Row.class);

		columnSlice = mock(ColumnSlice.class);
		columnSlice1 = mock(ColumnSlice.class);
		columnSlice2 = mock(ColumnSlice.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
				DataHubCompositeValueSerializer.get())).thenReturn(rangeQuery);
		when(rangeQuery.setColumnFamily(channelName)).thenReturn(rangeQuery);
		when(rangeQuery.execute()).thenReturn(queryResult);

		when(row1.getKey()).thenReturn("20130401");
		when(row2.getKey()).thenReturn("20130402");
		when(row.getColumnSlice()).thenReturn(columnSlice);
		when(latestItemRow.getKey()).thenReturn(CHANNELS_LATEST_ROW_KEY);
		when(row1.getColumnSlice()).thenReturn(columnSlice1);
		when(row2.getColumnSlice()).thenReturn(columnSlice2);
	}

	@Test
	public void testFindPrevious_simple() throws Exception {
		List<Row> rows = Arrays.asList(row);

		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(orderedRows);
		when(orderedRows.getList()).thenReturn(rows);
		when(columnSlice.getColumns()).thenReturn(Arrays.asList(column, previousColumn));
		when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
		when(previousColumn.getName()).thenReturn(keyRenderer.keyToString(expectedPrevious));

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

		assertEquals(expectedPrevious, result.get());
	}

	@Test
	public void testFindNext_includeLatestItemRow() throws Exception {
		List<Row> rows = Arrays.asList(latestItemRow, row);

		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(orderedRows);
		when(orderedRows.getList()).thenReturn(rows);
		when(columnSlice.getColumns()).thenReturn(Arrays.asList(column, previousColumn));
		when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
		when(previousColumn.getName()).thenReturn(keyRenderer.keyToString(expectedPrevious));

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

		assertEquals(expectedPrevious, result.get());
	}

	@Test
	public void testFindPrevious_spanRows() throws Exception {
		OrderedRows rows = mock(OrderedRows.class);
		List<Row> rowList = Arrays.asList(row1, row2);

		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(rows);
		when(rows.getList()).thenReturn(rowList);
		when(columnSlice1.getColumns()).thenReturn(Arrays.asList(column));
		when(columnSlice2.getColumns()).thenReturn(Arrays.asList(previousColumn));
		when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
		when(previousColumn.getName()).thenReturn(keyRenderer.keyToString(expectedPrevious));

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

		assertEquals(expectedPrevious, result.get());
	}

	@Test
	public void testFindPrevious_spanRows_secondRowHasMultipleColumns() throws Exception {
		OrderedRows rows = mock(OrderedRows.class);
		List<Row> rowList = Arrays.asList(row1, row2);

		HColumn<String, DataHubCompositeValue> someOtherPreviousColumn = mock(HColumn.class);

		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(rows);
		when(rows.getList()).thenReturn(rowList);
		when(columnSlice1.getColumns()).thenReturn(Arrays.asList(someOtherPreviousColumn));
		when(columnSlice2.getColumns()).thenReturn(Arrays.asList(previousColumn, column));
		when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
		when(previousColumn.getName()).thenReturn(keyRenderer.keyToString(expectedPrevious));

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

		assertEquals(expectedPrevious, result.get());
	}

	@Test
	public void testFindPrevious_notFound() throws Exception {
		OrderedRows rows = mock(OrderedRows.class);

		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(rows);
		when(rows.iterator()).thenReturn(Collections.emptyListIterator());

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

		assertFalse(result.isPresent());
	}

	@Test
	public void testFindNext_simple() throws Exception {
		OrderedRows rows = mock(OrderedRows.class);

		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MAX_KEY), false, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(rows);
		when(rows.getList()).thenReturn(Arrays.asList(row));
		when(columnSlice.getColumns()).thenReturn(Arrays.asList(column, nextColumn));
		when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
		when(nextColumn.getName()).thenReturn(keyRenderer.keyToString(expectedNext));

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findNext(channelName, targetKey);

		assertEquals(expectedNext, result.get());
	}

	@Test
	public void testFindNext_spanRows() throws Exception {
		OrderedRows rows = mock(OrderedRows.class);
		List<Row> rowList = Arrays.asList(row1, row2);
		when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MAX_KEY), false, 2)).thenReturn(rangeQuery);
		when(queryResult.get()).thenReturn(rows);
		when(rows.getList()).thenReturn(rowList);
		when(columnSlice1.getColumns()).thenReturn(Arrays.asList(column));
		when(columnSlice2.getColumns()).thenReturn(Arrays.asList(nextColumn));
		when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
		when(nextColumn.getName()).thenReturn(keyRenderer.keyToString(expectedNext));

		CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
		Optional<DataHubKey> result = testClass.findNext(channelName, targetKey);

		assertEquals(expectedNext, result.get());
	}
}
