package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraValueReaderTest {

    @Test
    public void testRead() throws Exception {

        String channelName = "spoon";
        UUID uid = UUID.randomUUID();
        byte[] data = new byte[]{'t', 'e', 's', 't', 'i', 'n'};
        String key = "the_____key___";
        DataHubCompositeValue expected = new DataHubCompositeValue("text/plain", data);

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, UUID, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);
        Keyspace keyspace = mock(Keyspace.class);
        ColumnQuery<String, UUID, DataHubCompositeValue> query = mock(ColumnQuery.class);
        QueryResult<HColumn<UUID, DataHubCompositeValue>> queryResult = mock(QueryResult.class);
        HColumn<UUID, DataHubCompositeValue> column = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createColumnQuery(keyspace, StringSerializer.get(), UUIDSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(
                query);
        when(rowKeyStrategy.buildKey(channelName, uid)).thenReturn(key);
        when(query.setKey(key)).thenReturn(query);
        when(query.setColumnFamily(channelName)).thenReturn(query);
        when(query.setName(uid)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(column);
        when(column.getValue()).thenReturn(expected);

        CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, null);

        DataHubCompositeValue result = testClass.read(channelName, uid);
        assertEquals(expected, result);
    }

    @Test
    public void testReadNotFound() throws Exception {
        String channelName = "spoon";
        UUID uid = UUID.randomUUID();
        String key = "the_____key___";

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, UUID, DataHubCompositeValue> rowKeyStrategy = mock(RowKeyStrategy.class);
        Keyspace keyspace = mock(Keyspace.class);
        ColumnQuery<String, UUID, DataHubCompositeValue> query = mock(ColumnQuery.class);
        QueryResult<HColumn<UUID, DataHubCompositeValue>> queryResult = mock(QueryResult.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createColumnQuery(keyspace, StringSerializer.get(), UUIDSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(
                query);
        when(rowKeyStrategy.buildKey(channelName, uid)).thenReturn(key);
        when(query.setKey(key)).thenReturn(query);
        when(query.setColumnFamily(channelName)).thenReturn(query);
        when(query.setName(uid)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(null);

        CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, null);

        DataHubCompositeValue result = testClass.read(channelName, uid);
        assertNull(result);
    }
}
