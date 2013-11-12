package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.datahub.util.TimeProvider;
import me.prettyprint.cassandra.model.HColumnImpl;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.CountQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CassandraChannelsCollectionTest {

    private CassandraConnector connector;
    private HectorFactoryWrapper hector;
    private Mutator<String> mutator;
    private Serializer<ChannelConfiguration> valueSerializer;
    private TimeProvider timeProvider;
    private DataHubKeyRenderer keyRenderer;
    private Keyspace keyspace;
    private ConcurrentMap<String,ChannelConfiguration> channelConfigurationMap;

    @Before
    public void setup() {
        connector = mock(CassandraConnector.class);
        hector = mock(HectorFactoryWrapper.class);
        mutator = mock(Mutator.class);
        valueSerializer = mock(Serializer.class);
        timeProvider = mock(TimeProvider.class);
        keyRenderer = new DataHubKeyRenderer();
        keyspace = mock(Keyspace.class);
        channelConfigurationMap = new ConcurrentHashMap<>();
    }

    @Test
    public void testCreateChannel() throws Exception {
        String channelName = "arturo";
        final Date creationDate = new Date(99999);
        ChannelConfiguration expected = new ChannelConfiguration(channelName, creationDate, null);
        HColumn<String, ChannelConfiguration> column = new HColumnImpl<String, ChannelConfiguration>(StringSerializer.get(), mock(Serializer.class));

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.createColumn(channelName, expected, StringSerializer.get(), valueSerializer)).thenReturn(column);
        when(timeProvider.getDate()).thenReturn(creationDate);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, valueSerializer, hector, timeProvider, keyRenderer, channelConfigurationMap);

        ChannelConfiguration result = testClass.createChannel(channelName, null);

        assertEquals(expected, result);
        verify(connector, never()).createColumnFamily(anyString());
        verify(mutator).insert(CHANNELS_ROW_KEY, CHANNELS_METADATA_COLUMN_FAMILY_NAME, column);
        assertEquals(result, channelConfigurationMap.get(channelName));
    }

    @Test
    public void testUpdateChannel() throws Exception {
        String channelName = "arturo";
        final Date creationDate = new Date(99999);
        ChannelConfiguration newConfig = new ChannelConfiguration(channelName, creationDate, null);
        HColumn<String, ChannelConfiguration> column = new HColumnImpl<String, ChannelConfiguration>(StringSerializer.get(), mock(Serializer.class));

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.createColumn(channelName, newConfig, StringSerializer.get(), valueSerializer)).thenReturn(column);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, valueSerializer, hector, timeProvider, keyRenderer, channelConfigurationMap);

        testClass.updateChannel(newConfig);
        verify(mutator).insert(CHANNELS_ROW_KEY, CHANNELS_METADATA_COLUMN_FAMILY_NAME, column);
        assertEquals(newConfig, channelConfigurationMap.get(channelName));
    }

    @Test
    public void testChannelExists() throws Exception {
        String channelName = "foo";
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, new Date(), null);

        Serializer<ChannelConfiguration> channelConfigSerializer = mock(Serializer.class);
        ColumnQuery<String, String, ChannelConfiguration> query = mock(ColumnQuery.class);
        QueryResult<HColumn<String, ChannelConfiguration>> queryResult = mock(QueryResult.class);
        HColumn<String, ChannelConfiguration> column = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), channelConfigSerializer)).thenReturn(query);
        when(query.setName(channelName)).thenReturn(query);
        when(query.setKey(CHANNELS_ROW_KEY)).thenReturn(query);
        when(query.setColumnFamily(CHANNELS_METADATA_COLUMN_FAMILY_NAME)).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(column);
        when(column.getValue()).thenReturn(channelConfiguration);


        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, channelConfigSerializer, hector, null, keyRenderer, channelConfigurationMap);
        boolean result = testClass.channelExists(channelName);
        assertTrue(result);
    }

    @Test
    public void testGetChannelConfiguration() throws Exception {
        ChannelConfiguration expected = new ChannelConfiguration("thechan", new Date(), null);

        Serializer<ChannelConfiguration> channelConfigSerializer = mock(Serializer.class);
        ColumnQuery<String, String, ChannelConfiguration> columnQuery = mock(ColumnQuery.class);
        QueryResult<HColumn<String, ChannelConfiguration>> queryResult = mock(QueryResult.class);
        HColumn<String, ChannelConfiguration> column = mock(HColumn.class);

        when(columnQuery.setName(anyString())).thenReturn(columnQuery);
        when(columnQuery.setKey(anyString())).thenReturn(columnQuery);
        when(columnQuery.setColumnFamily(anyString())).thenReturn(columnQuery);
        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), channelConfigSerializer)).thenReturn(columnQuery);
        when(columnQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(column);
        when(column.getValue()).thenReturn(expected);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, channelConfigSerializer, hector, null, keyRenderer, channelConfigurationMap);

        ChannelConfiguration result = testClass.getChannelConfiguration("thechan");

        assertEquals(expected, result);
        assertEquals(expected, channelConfigurationMap.get("thechan"));
    }

    @Test
    public void testCache() {
        String channel = "cashChan";
        ChannelConfiguration expected = new ChannelConfiguration(channel, new Date(), 1000L);
        channelConfigurationMap.put(channel, expected);
        CassandraChannelsCollection testClass = new CassandraChannelsCollection(null, null, null, null, null, channelConfigurationMap);
        ChannelConfiguration result = testClass.getChannelConfiguration(channel);
        assertEquals(expected, result);

    }

    @Test
    public void testUpdateFirstKey() throws Exception {
        String channelName = "myChan";
        DataHubKey key = new DataHubKey((short) 1000);
        String keyString = new DataHubKeyRenderer().keyToString(key);

        Serializer<ChannelConfiguration> configSerializer = mock(Serializer.class);
        HColumn<String, String> newColumn = mock(HColumn.class);
        Mutator<String> mutator = mock(Mutator.class);

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.createColumn(channelName, keyString, StringSerializer.get(), StringSerializer.get())).thenReturn(newColumn);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, configSerializer, hector, timeProvider, keyRenderer, channelConfigurationMap);
        testClass.updateFirstKey(channelName, key);

        verify(mutator).insert(channelName + ":" + CHANNELS_FIRST_ROW_KEY, DATA_HUB_COLUMN_FAMILY_NAME, newColumn);
    }

    @Test
    public void testDeleteFirstKey() throws Exception {
        String channelName = "myChan";
        Serializer<ChannelConfiguration> configSerializer = mock(Serializer.class);
        Mutator<String> mutator = mock(Mutator.class);

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, configSerializer, hector, timeProvider, keyRenderer, channelConfigurationMap);
        testClass.deleteFirstKey(channelName);

        verify(mutator).delete(channelName + ":" + CHANNELS_FIRST_ROW_KEY, DATA_HUB_COLUMN_FAMILY_NAME, "myChan", StringSerializer.get());
    }

    @Test
    public void testCountChannels() throws Exception {
        CountQuery<String, String> countQuery = mock(CountQuery.class);
        QueryResult<Integer> queryResult = mock(QueryResult.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createCountQuery(keyspace, StringSerializer.get(), StringSerializer.get())).thenReturn(countQuery);
        when(countQuery.setColumnFamily(CHANNELS_METADATA_COLUMN_FAMILY_NAME)).thenReturn(countQuery);
        when(countQuery.setKey(CHANNELS_ROW_KEY)).thenReturn(countQuery);
        when(countQuery.setRange(null, null, Integer.MAX_VALUE)).thenReturn(countQuery);
        when(countQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(5);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, null, hector, null, keyRenderer, channelConfigurationMap);
        int result = testClass.countChannels();
        assertEquals(5, result);
    }

    @Test
    public void testGetFirstKey() throws Exception {
        //GIVEN
        String channelName = "chunder";
        DataHubKey expected = new DataHubKey(1000);
        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, null, hector, null, keyRenderer, channelConfigurationMap);

        ColumnQuery<String, String, String> columnQuery = mock(ColumnQuery.class);
        QueryResult<HColumn<String, String>> queryResult = mock(QueryResult.class);
        HColumn<String, String> column = mock(HColumn.class);

        when(connector.getKeyspace()).thenReturn(keyspace);
        when(columnQuery.setName(channelName)).thenReturn(columnQuery);
        when(columnQuery.setKey(channelName + ":" + CassandraChannelsCollection.CHANNELS_FIRST_ROW_KEY)).thenReturn(columnQuery);
        when(columnQuery.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)).thenReturn(columnQuery);
        when(columnQuery.execute()).thenReturn(queryResult);
        when(queryResult.get()).thenReturn(column);
        when(column.getValue()).thenReturn(keyRenderer.keyToString(expected));

        when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())).thenReturn(columnQuery);

        //WHEN
        DataHubKey result = testClass.getFirstKey(channelName);

        //THEN
        assertEquals(expected, result);
    }

    @Test
    public void testGetChannels() throws Exception {
        //GIVEN
        ChannelConfiguration expected1 = new ChannelConfiguration("one", null, null);
        ChannelConfiguration expected2 = new ChannelConfiguration("two", null, null);

        SliceQuery<String, String, ChannelConfiguration> sliceQuery = mock(SliceQuery.class, RETURNS_DEEP_STUBS);
        HColumn column1 = mock(HColumn.class);
        HColumn column2 = mock(HColumn.class);
        ColumnSliceIterator<String, String, ChannelConfiguration> sliceIterator = mock(ColumnSliceIterator.class);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, valueSerializer, hector, null, keyRenderer, channelConfigurationMap);

        //WHEN
        when(column1.getValue()).thenReturn(expected1);
        when(column2.getValue()).thenReturn(expected2);
        when(connector.getKeyspace()).thenReturn(keyspace);
        when(hector.createSliceQuery(keyspace, StringSerializer.get(), StringSerializer.get(), valueSerializer)).thenReturn(sliceQuery);
        when(sliceQuery.setKey(CHANNELS_ROW_KEY)).thenReturn(sliceQuery);
        when(sliceQuery.setColumnFamily(CHANNELS_METADATA_COLUMN_FAMILY_NAME)).thenReturn(sliceQuery);
        when(hector.createColumnSliceIterator(sliceQuery, null, CassandraChannelsCollection.MAX_CHANNEL_NAME, false)).thenReturn(sliceIterator);
        when(sliceIterator.hasNext()).thenReturn(true, true, false);
        when(sliceIterator.next()).thenReturn(column1, column2);

        Iterable<ChannelConfiguration> result = testClass.getChannels();

        //THEN
        Iterator<ChannelConfiguration> iterator = result.iterator();
        assertEquals(expected1, iterator.next());
        assertEquals(expected2, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testUpdateLatestRowKey() throws Exception {
        String channelName = "chunnel";
        DataHubKey key = new DataHubKey(1000);
        SequenceRowKeyStrategy rowKeyStrategy = new SequenceRowKeyStrategy();
        String rowKey = rowKeyStrategy.buildKey(channelName, key);

        Serializer<ChannelConfiguration> configSerializer = mock(Serializer.class);
        HColumn<String, String> newColumn = mock(HColumn.class);
        Mutator<String> mutator = mock(Mutator.class);

        when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
        when(hector.createColumn(channelName, rowKey, StringSerializer.get(), StringSerializer.get())).thenReturn(newColumn);

        CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, configSerializer, hector, timeProvider, keyRenderer, channelConfigurationMap);
        testClass.updateLatestRowKey(channelName, rowKey);

        verify(mutator).insert(channelName + ":" + CHANNELS_LATEST_ROW_KEY, DATA_HUB_COLUMN_FAMILY_NAME, newColumn);
    }
}
