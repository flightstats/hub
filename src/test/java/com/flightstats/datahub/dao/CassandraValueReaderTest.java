package com.flightstats.datahub.dao;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
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

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, UUID, byte[]> rowKeyStrategy = mock(RowKeyStrategy.class);
        Keyspace keyspace = mock(Keyspace.class);
        ColumnQuery<String, UUID, byte[]> query = mock(ColumnQuery.class);
        QueryResult<HColumn<UUID, byte[]>> queryResult = mock(QueryResult.class);
        HColumn<UUID, byte[]> column = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createColumnQuery(keyspace, StringSerializer.get(), UUIDSerializer.get(), BytesArraySerializer.get())).thenReturn(query);
        when(rowKeyStrategy.buildKey(channelName, uid)).thenReturn(key);
        when(query.setKey(key)).thenReturn(query);
        when(query.setColumnFamily(channelName)).thenReturn(query);
        when(query.setName(uid)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(column);
        when(column.getValue()).thenReturn(data);

        CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy);

        byte[] result = testClass.read(channelName, uid);
        assertEquals(data, result);
    }

    @Test
    public void testReadNotFound() throws Exception {
        String channelName = "spoon";
        UUID uid = UUID.randomUUID();
        String key = "the_____key___";

        CassandraConnector connector = mock(CassandraConnector.class);
        HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
        RowKeyStrategy<String, UUID, byte[]> rowKeyStrategy = mock(RowKeyStrategy.class);
        Keyspace keyspace = mock(Keyspace.class);
        ColumnQuery<String, UUID, byte[]> query = mock(ColumnQuery.class);
        QueryResult<HColumn<UUID, byte[]>> queryResult = mock(QueryResult.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createColumnQuery(keyspace, StringSerializer.get(), UUIDSerializer.get(), BytesArraySerializer.get())).thenReturn(query);
        when(rowKeyStrategy.buildKey(channelName, uid)).thenReturn(key);
        when(query.setKey(key)).thenReturn(query);
        when(query.setColumnFamily(channelName)).thenReturn(query);
        when(query.setName(uid)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(null);

        CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy);

        byte[] result = testClass.read(channelName, uid);
        assertNull(result);
    }
}
