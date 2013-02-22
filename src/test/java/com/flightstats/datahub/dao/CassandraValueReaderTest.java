package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraValueReaderTest {

    @Test
    public void testRead() throws Exception {

        String channelName = "spoon";
        DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);
        byte[] data = new byte[]{'t', 'e', 's', 't', 'i', 'n'};
        String rowKey = "the_____key___";
        DataHubCompositeValue expected = new DataHubCompositeValue("text/plain", data);
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
        when(query.setColumnFamily(channelName)).thenReturn(query);
        when(query.setName(columnName)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(column);
        when(column.getValue()).thenReturn(expected);

        CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, null, keyRenderer);

        DataHubCompositeValue result = testClass.read(channelName, key);
        assertEquals(expected, result);
    }

    @Test
    public void testReadNotFound() throws Exception {
        String channelName = "spoon";
        DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);
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
        when(query.setColumnFamily(channelName)).thenReturn(query);
        when(query.setName(columnName)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(null);

        CassandraValueReader testClass = new CassandraValueReader(connector, hector, rowKeyStrategy, null, keyRenderer);

        DataHubCompositeValue result = testClass.read(channelName, key);
        assertNull(result);
    }

    @Test
    public void testFindLatestId() throws Exception {
        DataHubKey expected = new DataHubKey(new Date(999999999), (short) 6);
        String channelName = "myChan";
        ChannelConfiguration config = new ChannelConfiguration(channelName, null, expected);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

        when(channelsCollection.getChannelConfiguration(channelName)).thenReturn(config);

        CassandraValueReader testClass = new CassandraValueReader(null, null, null, channelsCollection, null);

        Optional<DataHubKey> result = testClass.findLatestId(channelName);
        assertEquals(expected, result.get());
    }

    @Test
    public void testFindLatestId_notFound() throws Exception {
        String channelName = "myChan";
        ChannelConfiguration config = new ChannelConfiguration(channelName, null, null);

        CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

        when(channelsCollection.getChannelConfiguration(channelName)).thenReturn(config);

        CassandraValueReader testClass = new CassandraValueReader(null, null, null, channelsCollection, null);
        Optional<DataHubKey> result = testClass.findLatestId(channelName);

        assertEquals(Optional.absent(), result);

    }
}
