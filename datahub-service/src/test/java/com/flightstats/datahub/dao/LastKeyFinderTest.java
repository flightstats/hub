package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
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
import java.util.List;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LastKeyFinderTest {

    private final static String CHANNEL_NAME = "supadupaunitspandadotcom";

    @Test
    public void testMissing() throws Exception {
        DataHubKey expected = new DataHubKey(9999);
        SequenceRowKeyStrategy rowKeyStrategy = new SequenceRowKeyStrategy();
        String rowKey = rowKeyStrategy.buildKey(CHANNEL_NAME, expected);
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        CassandraConnector cassandraConnector = mock(CassandraConnector.class);
        Keyspace keyspace = mock(Keyspace.class);
        RangeSlicesQuery<String, String, DataHubCompositeValue> query1 = mock(RangeSlicesQuery.class);

        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> query1Result = mock(QueryResult.class);
        OrderedRows<String, String, DataHubCompositeValue> query1Rows = mock(OrderedRows.class);
        Row<String, String, DataHubCompositeValue> query1Row1 = mock(Row.class);
        List<Row<String, String, DataHubCompositeValue>> row1List = Arrays.asList(query1Row1);
        ColumnSlice<String, DataHubCompositeValue> query1ColumnSlice = mock(ColumnSlice.class);

        when(channelsCollection.getLatestRowKey(CHANNEL_NAME)).thenReturn(rowKey);

        when(cassandraConnector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get()))
                .thenReturn(query1);

        when(query1.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)).thenReturn(query1);
        when(query1.setRange(null, null, true, 1)).thenReturn(query1);
        when(query1.setKeys(rowKey, rowKey)).thenReturn(query1);
        when(query1.execute()).thenReturn(query1Result);
        when(query1Result.get()).thenReturn(query1Rows);
        when(query1Rows.getList()).thenReturn(row1List);
        when(query1Row1.getColumnSlice()).thenReturn(query1ColumnSlice);
        when(query1ColumnSlice.getColumns()).thenReturn(Collections.<HColumn<String, DataHubCompositeValue>>emptyList());

        LastKeyFinder testClass = new LastKeyFinder(channelsCollection, hector, keyRenderer, cassandraConnector);

        assertNull(testClass.queryForLatestKey(CHANNEL_NAME));
    }

    @Test
	public void testFound() throws Exception {
		DataHubKey expected = new DataHubKey(9999);
        SequenceRowKeyStrategy rowKeyStrategy = new SequenceRowKeyStrategy();
        String rowKey = rowKeyStrategy.buildKey(CHANNEL_NAME, expected);
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		CassandraConnector cassandraConnector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		RangeSlicesQuery<String, String, DataHubCompositeValue> query1 = mock(RangeSlicesQuery.class);

		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> query1Result = mock(QueryResult.class);
        OrderedRows<String, String, DataHubCompositeValue> query1Rows = mock(OrderedRows.class);
        Row<String, String, DataHubCompositeValue> query1Row1 = mock(Row.class);
        List<Row<String, String, DataHubCompositeValue>> row1List = Arrays.asList(query1Row1);
        ColumnSlice<String, DataHubCompositeValue> query1ColumnSlice = mock(ColumnSlice.class);
		HColumn col1 = mock(HColumn.class);

		when(channelsCollection.getLatestRowKey(CHANNEL_NAME)).thenReturn(rowKey);

		when(cassandraConnector.getKeyspace()).thenReturn(keyspace);
		when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get()))
                .thenReturn(query1);

		when(query1.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)).thenReturn(query1);
		when(query1.setRange(null, null, true, 1)).thenReturn(query1);
		when(query1.setKeys(rowKey, rowKey)).thenReturn(query1);
		when(query1.execute()).thenReturn(query1Result);
		when(query1Result.get()).thenReturn(query1Rows);
		when(query1Rows.getList()).thenReturn(row1List);
		when(query1Row1.getColumnSlice()).thenReturn(query1ColumnSlice);
		when(query1ColumnSlice.getColumns()).thenReturn(Arrays.<HColumn<String, DataHubCompositeValue>>asList(col1));
        when(col1.getName()).thenReturn(keyRenderer.keyToString(expected));

		LastKeyFinder testClass = new LastKeyFinder(channelsCollection, hector, keyRenderer, cassandraConnector);

        assertEquals(expected, testClass.queryForLatestKey(CHANNEL_NAME));
	}
}
