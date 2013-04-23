package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.TimeProvider;
import me.prettyprint.cassandra.model.HColumnImpl;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.CountQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.junit.Test;

import java.util.Date;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.CHANNELS_COLUMN_FAMILY_NAME;
import static com.flightstats.datahub.dao.CassandraChannelsCollection.CHANNELS_ROW_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CassandraChannelsCollectionTest {

	@Test
	public void testCreateChannel() throws Exception {
		String channelName = "arturo";
		final Date creationDate = new Date(99999);
		ChannelConfiguration expected = new ChannelConfiguration(channelName, creationDate, null);
		HColumn<String, ChannelConfiguration> column = new HColumnImpl<String, ChannelConfiguration>(StringSerializer.get(), mock(Serializer.class));

		CassandraConnector connector = mock(CassandraConnector.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		Mutator<String> mutator = mock(Mutator.class);
		Serializer<ChannelConfiguration> valueSerializer = mock(Serializer.class);
		TimeProvider timeProvider = mock(TimeProvider.class);

		when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
		when(hector.createColumn(channelName, expected, StringSerializer.get(), valueSerializer)).thenReturn(column);
		when(timeProvider.getDate()).thenReturn(creationDate);

		CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, valueSerializer, hector, timeProvider);

		ChannelConfiguration result = testClass.createChannel(channelName);

		assertEquals(expected, result);
		verify(connector).createColumnFamily(channelName);
		verify(mutator).insert(CHANNELS_ROW_KEY, CHANNELS_COLUMN_FAMILY_NAME, column);
	}

	@Test
	public void testChannelExists() throws Exception {
		String channelName = "foo";
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, new Date(), null);

		CassandraConnector connector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		Serializer<ChannelConfiguration> channelConfigSerializer = mock(Serializer.class);
		ColumnQuery<String, String, ChannelConfiguration> query = mock(ColumnQuery.class);
		QueryResult<HColumn<String, ChannelConfiguration>> queryResult = mock(QueryResult.class);
		HColumn<String, ChannelConfiguration> column = mock(HColumn.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), channelConfigSerializer)).thenReturn(query);
		when(query.setName(channelName)).thenReturn(query);
		when(query.setKey(CHANNELS_ROW_KEY)).thenReturn(query);
		when(query.setColumnFamily(CHANNELS_COLUMN_FAMILY_NAME)).thenReturn(query);
		when(query.execute()).thenReturn(queryResult);
		when(queryResult.get()).thenReturn(column);
		when(column.getValue()).thenReturn(channelConfiguration);


		CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, channelConfigSerializer, hector, null);
		boolean result = testClass.channelExists(channelName);
		assertTrue(result);
	}

	@Test
	public void testGetChannelConfiguration() throws Exception {
		ChannelConfiguration expected = new ChannelConfiguration("thechan", new Date(), null);

		CassandraConnector connector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
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

		CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, channelConfigSerializer, hector, null);

		ChannelConfiguration result = testClass.getChannelConfiguration("thechan");

		assertEquals(expected, result);
	}

	@Test
	public void testUpdateLastUpdateTime() throws Exception {
		String channelName = "myChan";
		Date newDate = new Date(123456789L);
		Date creationDate = new Date();
		DataHubKey latestKey = new DataHubKey(newDate, (short) 0);
		ChannelConfiguration channelConfig = new ChannelConfiguration(channelName, creationDate, null);
		ChannelConfiguration expectedChannelConfig = new ChannelConfiguration(channelName, creationDate, latestKey);

		TimeProvider timeProvider = mock(TimeProvider.class);
		CassandraConnector connector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		ColumnQuery<String, String, ChannelConfiguration> query = mock(ColumnQuery.class);
		Serializer<ChannelConfiguration> configSerializer = mock(Serializer.class);
		QueryResult<HColumn<String, ChannelConfiguration>> queryResult = mock(QueryResult.class);
		HColumn<String, ChannelConfiguration> existingColumn = mock(HColumn.class);
		HColumn<String, ChannelConfiguration> newColumn = mock(HColumn.class);
		Mutator<String> mutator = mock(Mutator.class);

		when(timeProvider.getDate()).thenReturn(newDate);
		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(), configSerializer)).thenReturn(query);
		when(query.setName(anyString())).thenReturn(query);
		when(query.setKey(anyString())).thenReturn(query);
		when(query.setColumnFamily(anyString())).thenReturn(query);
		when(query.execute()).thenReturn(queryResult);
		when(queryResult.get()).thenReturn(existingColumn);
		when(existingColumn.getValue()).thenReturn(channelConfig);
		when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
		when(hector.createColumn(channelName, expectedChannelConfig, StringSerializer.get(), configSerializer)).thenReturn(newColumn);

		CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, configSerializer, hector, timeProvider);
		DataHubKey key = new DataHubKey(newDate, (short) 0);
		testClass.updateLastUpdatedKey(channelName, key);

		verify(mutator).insert(CHANNELS_ROW_KEY, CHANNELS_COLUMN_FAMILY_NAME, newColumn);
	}

	@Test
	public void testCountChannels() throws Exception {
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		Keyspace keyspace = mock(Keyspace.class);
		CassandraConnector connector = mock(CassandraConnector.class);
		CountQuery<String, String> countQuery = mock(CountQuery.class);
		QueryResult<Integer> queryResult = mock(QueryResult.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createCountQuery(keyspace, StringSerializer.get(), StringSerializer.get())).thenReturn(countQuery);
		when(countQuery.setColumnFamily(CHANNELS_COLUMN_FAMILY_NAME)).thenReturn(countQuery);
		when(countQuery.setKey(CHANNELS_ROW_KEY)).thenReturn(countQuery);
		when(countQuery.setRange(null, null, Integer.MAX_VALUE)).thenReturn(countQuery);
		when(countQuery.execute()).thenReturn(queryResult);
		when(queryResult.get()).thenReturn(5);

		CassandraChannelsCollection testClass = new CassandraChannelsCollection(connector, null, hector, null);
		int result = testClass.countChannels();
		assertEquals(5, result);
	}
}
