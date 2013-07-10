package com.flightstats.datahub.dao;

import com.flightstats.datahub.dao.serialize.DataHubCompositeValueSerializer;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CassandraChannelDaoTest {

	@Test
	public void testChannelExists() throws Exception {
		CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
		when(collection.channelExists("thechan")).thenReturn(true);
		when(collection.channelExists("nope")).thenReturn(false);
		CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null, null, null, null, null);
		assertTrue(testClass.channelExists("thechan"));
		assertFalse(testClass.channelExists("nope"));
	}

	@Test
	public void testCreateChannel() throws Exception {
		ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999), null);
		CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
		when(collection.createChannel("foo", null)).thenReturn(expected);
		CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null, null, null, null, null);
		ChannelConfiguration result = testClass.createChannel("foo", null);
		assertEquals(expected, result);
	}

	@Test
	public void testUpdateChannel() throws Exception {
		ChannelConfiguration newConfig = new ChannelConfiguration("foo", new Date(9999), 30000L);
		CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
		CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null, null, null, null, null, null);
		testClass.updateChannelMetadata(newConfig);
		verify(collection).updateChannel(newConfig);
	}

	@Test
	public void testInsert() throws Exception {
		// GIVEN
		Date date = new Date(2345678910L);
		DataHubKey key = new DataHubKey(date, (short) 3);
		String channelName = "foo";
		byte[] data = "bar".getBytes();
		Optional<String> contentType = Optional.of("text/plain");
		DataHubCompositeValue value = new DataHubCompositeValue(contentType, Optional.<String>absent(), Optional.<String>absent(), data);
		ValueInsertionResult expected = new ValueInsertionResult(key);
		DataHubKey lastUpdateKey = new DataHubKey(new Date(2345678912L), (short) 0);

		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		CassandraValueWriter inserter = mock(CassandraValueWriter.class);
		CassandraValueReader reader = mock(CassandraValueReader.class);
		CassandraLinkagesCollection linkagesCollection = mock(CassandraLinkagesCollection.class);
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		IMap<Object, Object> hcMap = mock(IMap.class);

		// WHEN
		when(hazelcast.getMap(CassandraChannelDao.LAST_CHANNEL_UPDATE)).thenReturn(hcMap);
		when(hcMap.put(channelName, key)).thenReturn(lastUpdateKey);
		when(inserter.write(channelName, value)).thenReturn(new ValueInsertionResult(key));
		CassandraChannelDao testClass = spy(new CassandraChannelDao(channelsCollection, linkagesCollection, inserter, reader, null, null, null, null, hazelcast));
		doReturn(lastUpdateKey).when(testClass).initializeLastUpdatedCache(anyString());

		ValueInsertionResult result = testClass.insert(channelName, contentType, Optional.<String>absent(), Optional.<String>absent(), data);

		// THEN
		assertEquals(expected, result);
		verify(linkagesCollection).updateLinkages(channelName, result.getKey(), lastUpdateKey);
	}

	@Test
	public void testGetValue() throws Exception {
		String channelName = "cccccc";
		DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);
		DataHubKey previousKey = new DataHubKey(new Date(9998888777665L), (short) 0);
		DataHubKey nextKey = new DataHubKey(new Date(9998888777667L), (short) 0);
		byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
		DataHubCompositeValue compositeValue = new DataHubCompositeValue(Optional.of("text/plain"), null, null, data);
		Optional<DataHubKey> previous = Optional.of(previousKey);
		Optional<DataHubKey> next = Optional.of(nextKey);
		LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(compositeValue, previous, next);

		CassandraValueReader reader = mock(CassandraValueReader.class);
		CassandraLinkagesCollection linkagesCollection = mock(CassandraLinkagesCollection.class);

		when(reader.read(channelName, key)).thenReturn(compositeValue);
		when(linkagesCollection.findPreviousKey(channelName, key)).thenReturn(previous);
		when(linkagesCollection.findNextKey(channelName, key)).thenReturn(next);

		CassandraChannelDao testClass = new CassandraChannelDao(null, linkagesCollection, null, reader, null, null, null, null, null);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertEquals(expected, result.get());
	}

	@Test
	public void testGetValue_notFound() throws Exception {
		String channelName = "cccccc";
		DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);

		CassandraValueReader reader = mock(CassandraValueReader.class);

		when(reader.read(channelName, key)).thenReturn(null);

		CassandraChannelDao testClass = new CassandraChannelDao(null, null, null, reader, null, null, null, null, null);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertFalse(result.isPresent());
	}

	@Test
	public void testFindLatestId() throws Exception {
		DataHubKey expected = new DataHubKey(new Date(999999999), (short) 6);
		String channelName = "myChan";

		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		IMap<Object, Object> hcMap = mock(IMap.class);
		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

		when(hazelcast.getMap(CassandraChannelDao.LAST_CHANNEL_UPDATE)).thenReturn(hcMap);
		when(hcMap.get(channelName)).thenReturn(expected);

		CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, null, null, null, null, null, null, hazelcast);

		Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);
		assertEquals(expected, result.get());
	}

	@Test
	public void testFindLatestId_lastUpdateNotFound() throws Exception {
		// GIVEN
		String channelName = "myChan";
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		IMap<Object, Object> hcMap = mock(IMap.class);
		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);

		// WHEN
		when(hazelcast.getMap(CassandraChannelDao.LAST_CHANNEL_UPDATE)).thenReturn(hcMap);
		when(hcMap.get(channelName)).thenReturn(null);

		CassandraChannelDao testClass = spy(new CassandraChannelDao(channelsCollection, null, null, null, null, null, null, null, hazelcast));
		doReturn(null).when(testClass).initializeLastUpdatedCache(anyString());
		Optional<DataHubKey> result = testClass.findLastUpdatedKey(channelName);

		// THEN
		assertEquals(Optional.absent(), result);
	}

	@Test
	public void testFindKeysInRange() throws Exception {
		String channelName = "myChan";
		ChannelConfiguration config = new ChannelConfiguration(channelName, null, null);
		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		CassandraConnector connector = mock(CassandraConnector.class);
		Keyspace keyspace = mock(Keyspace.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy = new YearMonthDayRowKeyStrategy();
		RangeSlicesQuery<String, String, DataHubCompositeValue> query = mock(RangeSlicesQuery.class);
		QueryResult<OrderedRows<String, String, DataHubCompositeValue>> queryResults = mock(QueryResult.class);
		OrderedRows<String, String, DataHubCompositeValue> queryResultsGuts = mock(OrderedRows.class);

		when(connector.getKeyspace()).thenReturn(keyspace);
		when(hector.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(), DataHubCompositeValueSerializer.get())).thenReturn(query);
		when(query.setColumnFamily(anyString())).thenReturn(query);
		when(query.setKeys(anyString(), anyString())).thenReturn(query);
		when(query.setColumnFamily(anyString())).thenReturn(query);
		when(query.setRange(anyString(), anyString(), anyBoolean(), anyInt())).thenReturn(query);
		when(query.execute()).thenReturn(queryResults);
		when(queryResults.get()).thenReturn(queryResultsGuts);
		when(queryResultsGuts.getList()).thenReturn(Collections.<Row<String, String, DataHubCompositeValue>>emptyList());
		when(channelsCollection.getChannelConfiguration(channelName)).thenReturn(config);
		when(connector.getKeyspace()).thenReturn(keyspace);
		when(keyspace.getKeyspaceName()).thenReturn("datahub");

		CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, null, null, null, keyRenderer, rowKeyStrategy, connector, hector, null);
		Collection<DataHubKey> result = testClass.findKeysInRange(channelName, new Date(0), new Date());
		assertEquals(Collections.emptyList(), result);
	}
}
