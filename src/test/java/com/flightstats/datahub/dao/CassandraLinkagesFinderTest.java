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
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraLinkagesFinderTest {

    @Test
    public void testFindPrevious_simple() throws Exception {
        String channelName = "snapdragon";
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
        DataHubKey targetKey = new DataHubKey(new Date(555L), (short) 1);
        DataHubKey expectedPrevious = new DataHubKey(new Date(555L), (short) 0);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        Keyspace keyspace = mock(Keyspace.class);
        RangeSlicesQuery<String, String, DataHubCompositeValue> rangeQuery = mock(RangeSlicesQuery.class);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
        OrderedRows rows = mock(OrderedRows.class);
        Row row = mock(Row.class);
        Iterator rowsIterator = Arrays.asList(row).iterator();
        ColumnSlice columnSlice = mock(ColumnSlice.class);
        HColumn<String, DataHubCompositeValue> column = mock(HColumn.class);
        HColumn<String, DataHubCompositeValue> previousColumn = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get())).thenReturn(rangeQuery);
        when(rangeQuery.setColumnFamily(channelName)).thenReturn(rangeQuery);
        when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
        when(rangeQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(rows);
        when(rows.iterator()).thenReturn(rowsIterator);
        when(row.getColumnSlice()).thenReturn(columnSlice);
        when(columnSlice.getColumns()).thenReturn(Arrays.asList(column, previousColumn));
        when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
        when(previousColumn.getName()).thenReturn(keyRenderer.keyToString(expectedPrevious));

        CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
        Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

        assertEquals(expectedPrevious, result.get());
    }

    @Test
    public void testFindPrevious_spanRows() throws Exception {
        String channelName = "bling";
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
        DataHubKey targetKey = new DataHubKey(new Date(555L), (short) 1);
        DataHubKey expectedPrevious = new DataHubKey(new Date(555L), (short) 0);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        Keyspace keyspace = mock(Keyspace.class);
        RangeSlicesQuery<String, String, DataHubCompositeValue> rangeQuery = mock(RangeSlicesQuery.class);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
        OrderedRows rows = mock(OrderedRows.class);
        Row row1 = mock(Row.class);
        Row row2 = mock(Row.class);
        Iterator rowsIterator = Arrays.asList(row1, row2).iterator();
        ColumnSlice columnSlice1 = mock(ColumnSlice.class);
        ColumnSlice columnSlice2 = mock(ColumnSlice.class);
        HColumn<String, DataHubCompositeValue> column = mock(HColumn.class);
        HColumn<String, DataHubCompositeValue> previousColumn = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get())).thenReturn(rangeQuery);
        when(rangeQuery.setColumnFamily(channelName)).thenReturn(rangeQuery);
        when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
        when(rangeQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(rows);
        when(rows.iterator()).thenReturn(rowsIterator);
        when(row1.getColumnSlice()).thenReturn(columnSlice1);
        when(row2.getColumnSlice()).thenReturn(columnSlice2);
        when(columnSlice1.getColumns()).thenReturn(Arrays.asList(column));
        when(columnSlice2.getColumns()).thenReturn(Arrays.asList(previousColumn));
        when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
        when(previousColumn.getName()).thenReturn(keyRenderer.keyToString(expectedPrevious));

        CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
        Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

        assertEquals(expectedPrevious, result.get());
    }

    @Test
    public void testFindPrevious_notFound() throws Exception {
        String channelName = "bzzzzt";
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
        DataHubKey targetKey = new DataHubKey(new Date(555L), (short) 1);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        Keyspace keyspace = mock(Keyspace.class);
        RangeSlicesQuery<String, String, DataHubCompositeValue> rangeQuery = mock(RangeSlicesQuery.class);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
        OrderedRows rows = mock(OrderedRows.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get())).thenReturn(rangeQuery);
        when(rangeQuery.setColumnFamily(channelName)).thenReturn(rangeQuery);
        when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MIN_KEY), true, 2)).thenReturn(rangeQuery);
        when(rangeQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(rows);
        when(rows.iterator()).thenReturn(Collections.emptyListIterator());

        CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
        Optional<DataHubKey> result = testClass.findPrevious(channelName, targetKey);

        assertFalse(result.isPresent());
    }

    @Test
    public void testFindNext_simple() throws Exception {
        String channelName = "gobstopper";
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
        DataHubKey targetKey = new DataHubKey(new Date(555L), (short) 1);
        DataHubKey expectedNext = new DataHubKey(new Date(556L), (short) 0);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        Keyspace keyspace = mock(Keyspace.class);
        RangeSlicesQuery<String, String, DataHubCompositeValue> rangeQuery = mock(RangeSlicesQuery.class);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
        OrderedRows rows = mock(OrderedRows.class);
        Row row = mock(Row.class);
        Iterator rowsIterator = Arrays.asList(row).iterator();
        ColumnSlice columnSlice = mock(ColumnSlice.class);
        HColumn<String, DataHubCompositeValue> column = mock(HColumn.class);
        HColumn<String, DataHubCompositeValue> nextColumn = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get())).thenReturn(rangeQuery);
        when(rangeQuery.setColumnFamily(channelName)).thenReturn(rangeQuery);
        when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MAX_KEY), false, 2)).thenReturn(rangeQuery);
        when(rangeQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(rows);
        when(rows.iterator()).thenReturn(rowsIterator);
        when(row.getColumnSlice()).thenReturn(columnSlice);
        when(columnSlice.getColumns()).thenReturn(Arrays.asList(column, nextColumn));
        when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
        when(nextColumn.getName()).thenReturn(keyRenderer.keyToString(expectedNext));

        CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
        Optional<DataHubKey> result = testClass.findNext(channelName, targetKey);

        assertEquals(expectedNext, result.get());
    }

    @Test
    public void testFindNext_spanRows() throws Exception {
        String channelName = "gobstopper";
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
        DataHubKey targetKey = new DataHubKey(new Date(555L), (short) 1);
        DataHubKey expectedNext = new DataHubKey(new Date(556L), (short) 0);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        Keyspace keyspace = mock(Keyspace.class);
        RangeSlicesQuery<String, String, DataHubCompositeValue> rangeQuery = mock(RangeSlicesQuery.class);
        QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
        OrderedRows rows = mock(OrderedRows.class);
        Row row1 = mock(Row.class);
        Row row2 = mock(Row.class);
        Iterator rowsIterator = Arrays.asList(row1, row2).iterator();
        ColumnSlice columnSlice1 = mock(ColumnSlice.class);
        ColumnSlice columnSlice2 = mock(ColumnSlice.class);
        HColumn<String, DataHubCompositeValue> column = mock(HColumn.class);
        HColumn<String, DataHubCompositeValue> nextColumn = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                DataHubCompositeValueSerializer.get())).thenReturn(rangeQuery);
        when(rangeQuery.setColumnFamily(channelName)).thenReturn(rangeQuery);
        when(rangeQuery.setRange(keyRenderer.keyToString(targetKey), keyRenderer.keyToString(DataHubKey.MAX_KEY), false, 2)).thenReturn(rangeQuery);
        when(rangeQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(rows);
        when(rows.iterator()).thenReturn(rowsIterator);
        when(row1.getColumnSlice()).thenReturn(columnSlice1);
        when(row2.getColumnSlice()).thenReturn(columnSlice2);
        when(columnSlice1.getColumns()).thenReturn(Arrays.asList(column));
        when(columnSlice2.getColumns()).thenReturn(Arrays.asList(nextColumn));
        when(column.getName()).thenReturn(keyRenderer.keyToString(targetKey));
        when(nextColumn.getName()).thenReturn(keyRenderer.keyToString(expectedNext));

        CassandraLinkagesFinder testClass = new CassandraLinkagesFinder(connector, hector, keyRenderer);
        Optional<DataHubKey> result = testClass.findNext(channelName, targetKey);

        assertEquals(expectedNext, result.get());
    }
}
