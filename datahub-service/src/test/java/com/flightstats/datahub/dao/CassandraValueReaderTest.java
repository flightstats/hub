package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.junit.Test;

import java.util.Date;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraValueReaderTest {

	@Test
	public void testRead() throws Exception {

		String channelName = "spoon";
		DataHubKey key = new DataHubKey((short) 1000);
		byte[] data = new byte[]{'t', 'e', 's', 't', 'i', 'n'};
		String rowKey = "the_____key___";
		DataHubCompositeValue expected = new DataHubCompositeValue(Optional.of("text/plain"), null, data);
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		String columnName = keyRenderer.keyToString(key);

		CassandraConnector connector = mock(CassandraConnector.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);
		Keyspace keyspace = mock(Keyspace.class);
		ColumnQuery<String, String, DataHubCompositeValue> query = mock(ColumnQuery.class);
		QueryResult<HColumn<String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
		HColumn<String, DataHubCompositeValue> column = mock(HColumn.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(
				query);
		when(rowKeyStrategy.buildKey(channelName, key)).thenReturn(rowKey);
		when(query.setKey(rowKey)).thenReturn(query);
		when(query.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)).thenReturn(query);
		when(query.setName(columnName)).thenReturn(query);
		when(query.execute()).thenReturn(queryResult);
		when(queryResult.get()).thenReturn(column);
		when(column.getValue()).thenReturn(expected);

		CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, keyRenderer);

		DataHubCompositeValue result = testClass.read(channelName, key);
		assertEquals(expected, result);
	}

	@Test
	public void testReadNotFound() throws Exception {
		String channelName = "spoon";
		DataHubKey key = new DataHubKey((short) 1000);
		String rowKey = "the_____key___";
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		String columnName = keyRenderer.keyToString(key);

		CassandraConnector connector = mock(CassandraConnector.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);
		Keyspace keyspace = mock(Keyspace.class);
		ColumnQuery<String, String, DataHubCompositeValue> query = mock(ColumnQuery.class);
		QueryResult<HColumn<String, DataHubCompositeValue>> queryResult = mock(QueryResult.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(
				query);
		when(rowKeyStrategy.buildKey(channelName, key)).thenReturn(rowKey);
		when(query.setKey(rowKey)).thenReturn(query);
		when(query.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)).thenReturn(query);
		when(query.setName(columnName)).thenReturn(query);
		when(query.execute()).thenReturn(queryResult);
		when(queryResult.get()).thenReturn(null);

		CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, keyRenderer);

		DataHubCompositeValue result = testClass.read(channelName, key);
		assertNull(result);
	}

	@Test(expected = NoSuchChannelException.class)
	public void testRead_invalidChannel() throws Exception {
		String channelName = "myChan";
		String rowKey = "1234";
		DataHubKey key = new DataHubKey((short) 1000);
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		CassandraConnector connector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		ColumnQuery<String, String, DataHubCompositeValue> query = mock(ColumnQuery.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(
				query);
		when(query.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)).thenReturn(query);
		when(rowKeyStrategy.buildKey(channelName, key)).thenReturn(rowKey);
		when(query.setKey(rowKey)).thenReturn(query);
		when(query.setName(keyRenderer.keyToString(key))).thenReturn(query);
		when(query.execute()).thenThrow(new HInvalidRequestException("unconfigured columnfamily"));

		CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, keyRenderer);
		testClass.read(channelName, key);
	}

}
