package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.datahub.util.TimeProvider;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LastKeyFinderTest {

	private final static String CHANNEL_NAME = "supadupaunitspandadotcom";

	@Test
	public void testQueryLatest_noFirst() throws Exception {
		//GIVEN

		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

		when(channelsCollection.getFirstKey(CHANNEL_NAME)).thenReturn(null);

		LastKeyFinder testClass = new LastKeyFinder(channelsCollection, null, null, null, null, null);

		//WHEN
		DataHubKey result = testClass.queryForLatestKey(CHANNEL_NAME);

		//THEN
	    assertNull(result);
	}

	@Test
	public void testAllRowsMiss() throws Exception {
		//GIVEN
		Date now = new Date(677888L);
		DataHubKey firstKey = new DataHubKey(new Date(99999L), (short) 0);

		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);
		TimeProvider timeProvider = mock(TimeProvider.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		CassandraConnector cassandraConnector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		RangeSlicesQuery<String, String, DataHubCompositeValue> query1 = mock(RangeSlicesQuery.class);
		RangeSlicesQuery<String, String, DataHubCompositeValue> query2 = mock(RangeSlicesQuery.class);
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> query1Result = mock(QueryResult.class);
		OrderedRows<String, String, DataHubCompositeValue> query1Rows = mock(OrderedRows.class);
		Row<String, String, DataHubCompositeValue> query1Row1 = mock(Row.class);
		List<Row<String, String, DataHubCompositeValue>> row1List = Arrays.asList(query1Row1);
		ColumnSlice<String, DataHubCompositeValue> query1ColumnSlice = mock(ColumnSlice.class);
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> query2Result = mock(QueryResult.class);
		OrderedRows<String, String, DataHubCompositeValue> query2Rows = mock(OrderedRows.class);

		when(channelsCollection.getFirstKey(CHANNEL_NAME)).thenReturn(firstKey);
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, firstKey)).thenReturn("DAFIRSTROW");
		when(timeProvider.getDate()).thenReturn(now);
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, new DataHubKey(now, (short) 0))).thenReturn("NOW_ROW");
		when(rowKeyStrategy.nextKey(CHANNEL_NAME, "NOW_ROW")).thenReturn("TOMORROW_ROW");
		when(cassandraConnector.getKeyspace()).thenReturn(keyspace);
		when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(query1, query2);

		when(query1.setColumnFamily(CHANNEL_NAME)).thenReturn(query1);
		when(query1.setRange(null, null, true, 1)).thenReturn(query1);
		when(query1.setKeys("TOMORROW_ROW", "TOMORROW_ROW")).thenReturn(query1);
		when(query1.execute()).thenReturn(query1Result);
		when(query1Result.get()).thenReturn(query1Rows);
		when(query1Rows.getList()).thenReturn(row1List);
		when(query1Row1.getColumnSlice()).thenReturn(query1ColumnSlice);
		when(query1ColumnSlice.getColumns()).thenReturn(Collections.<HColumn<String, DataHubCompositeValue>>emptyList());

		when(rowKeyStrategy.prevKey(CHANNEL_NAME, "TOMORROW_ROW")).thenReturn("NOW_ROW");
		when(rowKeyStrategy.prevKey(CHANNEL_NAME, "NOW_ROW")).thenReturn("0BEFORE_FIRST");

		when(query2.setColumnFamily(CHANNEL_NAME)).thenReturn(query2);
		when(query2.setRange(null, null, true, 1)).thenReturn(query2);
		when(query2.setKeys("NOW_ROW", "NOW_ROW")).thenReturn(query2);
		when(query2.execute()).thenReturn(query2Result);
		when(query2Result.get()).thenReturn(query2Rows);
		when(query2Rows.getList()).thenReturn(Collections.<Row<String, String, DataHubCompositeValue>>emptyList());

		LastKeyFinder testClass = new LastKeyFinder(channelsCollection, hector, null, cassandraConnector, rowKeyStrategy, timeProvider);

		//WHEN
		DataHubKey result = testClass.queryForLatestKey(CHANNEL_NAME);

		//THEN
		assertNull(result);
	}

	@Test
	public void testFound() throws Exception {
		//GIVEN
		Date now = new Date(677888L);
		DataHubKey firstKey = new DataHubKey(new Date(99999L), (short) 0);
		DataHubKey expected = new DataHubKey(new Date(8888888L), (short)0);
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);
		TimeProvider timeProvider = mock(TimeProvider.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		CassandraConnector cassandraConnector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		RangeSlicesQuery<String, String, DataHubCompositeValue> query1 = mock(RangeSlicesQuery.class);
		RangeSlicesQuery<String, String, DataHubCompositeValue> query2 = mock(RangeSlicesQuery.class);
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> query1Result = mock(QueryResult.class);
		OrderedRows<String, String, DataHubCompositeValue> query1Rows = mock(OrderedRows.class);
		Row<String, String, DataHubCompositeValue> query1Row1 = mock(Row.class);
		List<Row<String, String, DataHubCompositeValue>> row1List = Arrays.asList(query1Row1);
		ColumnSlice<String, DataHubCompositeValue> query1ColumnSlice = mock(ColumnSlice.class);
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> query2Result = mock(QueryResult.class);
		OrderedRows<String, String, DataHubCompositeValue> query2Rows = mock(OrderedRows.class);
		Row<String, String, DataHubCompositeValue> query2Row1 = mock(Row.class);
		List<Row<String, String, DataHubCompositeValue>> row2List = Arrays.asList(query2Row1);
		ColumnSlice<String, DataHubCompositeValue> query2ColumnSlice = mock(ColumnSlice.class);
		HColumn col1 = mock(HColumn.class);

		when(channelsCollection.getFirstKey(CHANNEL_NAME)).thenReturn(firstKey);
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, firstKey)).thenReturn("DAFIRSTROW");
		when(timeProvider.getDate()).thenReturn(now);
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, new DataHubKey(now, (short) 0))).thenReturn("NOW_ROW");
		when(rowKeyStrategy.nextKey(CHANNEL_NAME, "NOW_ROW")).thenReturn("TOMORROW_ROW");
		when(cassandraConnector.getKeyspace()).thenReturn(keyspace);
		when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(query1, query2);

		when(query1.setColumnFamily(CHANNEL_NAME)).thenReturn(query1);
		when(query1.setRange(null, null, true, 1)).thenReturn(query1);
		when(query1.setKeys("TOMORROW_ROW", "TOMORROW_ROW")).thenReturn(query1);
		when(query1.execute()).thenReturn(query1Result);
		when(query1Result.get()).thenReturn(query1Rows);
		when(query1Rows.getList()).thenReturn(row1List);
		when(query1Row1.getColumnSlice()).thenReturn(query1ColumnSlice);
		when(query1ColumnSlice.getColumns()).thenReturn(Collections.<HColumn<String, DataHubCompositeValue>>emptyList());

		when(rowKeyStrategy.prevKey(CHANNEL_NAME, "TOMORROW_ROW")).thenReturn("NOW_ROW");
		when(rowKeyStrategy.prevKey(CHANNEL_NAME, "NOW_ROW")).thenReturn("0BEFORE_FIRST");

		when(query2.setColumnFamily(CHANNEL_NAME)).thenReturn(query2);
		when(query2.setRange(null, null, true, 1)).thenReturn(query2);
		when(query2.setKeys("NOW_ROW", "NOW_ROW")).thenReturn(query2);
		when(query2.execute()).thenReturn(query2Result);
		when(query2Result.get()).thenReturn(query2Rows);
		when(query2Rows.getList()).thenReturn(row2List);
		when(query2Row1.getColumnSlice()).thenReturn(query2ColumnSlice);
		when(query2ColumnSlice.getColumns()).thenReturn(Arrays.<HColumn<String, DataHubCompositeValue>>asList(col1));
		when(col1.getName()).thenReturn(keyRenderer.keyToString(expected));

		LastKeyFinder testClass = new LastKeyFinder(channelsCollection, hector, keyRenderer, cassandraConnector, rowKeyStrategy, timeProvider);

		//WHEN
		DataHubKey result = testClass.queryForLatestKey(CHANNEL_NAME);

		//THEN
		assertEquals(expected, result);
	}
}
