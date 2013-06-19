package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraChannelDaoTest {

	@Test
	public void testChannelExists() throws Exception {
		CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
		when(collection.channelExists("thechan")).thenReturn(true);
		when(collection.channelExists("nope")).thenReturn(false);
		CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null);
		assertTrue(testClass.channelExists("thechan"));
		assertFalse(testClass.channelExists("nope"));
	}

	@Test
	public void testCreateChannel() throws Exception {
		ChannelConfiguration expected = new ChannelConfiguration("foo", new Date(9999));
		CassandraChannelsCollection collection = mock(CassandraChannelsCollection.class);
		when(collection.createChannel("foo")).thenReturn(expected);
		CassandraChannelDao testClass = new CassandraChannelDao(collection, null, null, null);
		ChannelConfiguration result = testClass.createChannel("foo");
		assertEquals(expected, result);
	}

	@Test
	public void testInsert() throws Exception {
		Date date = new Date(2345678910L);
		DataHubKey key = new DataHubKey(date, (short) 3);
		String channelName = "foo";
		byte[] data = "bar".getBytes();
		String contentType = "text/plain";
		DataHubCompositeValue value = new DataHubCompositeValue(contentType, data);
		ValueInsertionResult expected = new ValueInsertionResult(key);

		CassandraChannelsCollection channelsCollection = mock(CassandraChannelsCollection.class);
		CassandraValueWriter inserter = mock(CassandraValueWriter.class);
		CassandraValueReader reader = mock(CassandraValueReader.class);

		when(inserter.write(channelName, value)).thenReturn(new ValueInsertionResult(key));
		when(reader.findFirstId(channelName)).thenReturn(Optional.<DataHubKey>absent());
		CassandraChannelDao testClass = new CassandraChannelDao(channelsCollection, inserter, reader, null);

		ValueInsertionResult result = testClass.insert(channelName, contentType, data);

		verify( channelsCollection ).updateFirstKey( "foo", key );
		assertEquals(expected, result);
	}

	@Test
	public void testGetValue() throws Exception {
		String channelName = "cccccc";
		DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);
		DataHubKey previousKey = new DataHubKey(new Date(9998888777665L), (short) 0);
		DataHubKey nextKey = new DataHubKey(new Date(9998888777667L), (short) 0);
		byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};
		DataHubCompositeValue compositeValue = new DataHubCompositeValue("text/plain", data);
		Optional<DataHubKey> previous = Optional.of(previousKey);
		Optional<DataHubKey> next = Optional.of(nextKey);
		LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(compositeValue, previous, next);

		CassandraValueReader reader = mock(CassandraValueReader.class);
		CassandraLinkagesFinder linkagesFinder = mock(CassandraLinkagesFinder.class);

		when(reader.read(channelName, key)).thenReturn(compositeValue);
		when(linkagesFinder.findPrevious(channelName, key)).thenReturn(previous);
		when(linkagesFinder.findNext(channelName, key)).thenReturn(next);

		CassandraChannelDao testClass = new CassandraChannelDao(null, null, reader, linkagesFinder);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertEquals(expected, result.get());
	}

	@Test
	public void testGetValue_notFound() throws Exception {
		String channelName = "cccccc";
		DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);

		CassandraValueReader reader = mock(CassandraValueReader.class);

		when(reader.read(channelName, key)).thenReturn(null);

		CassandraChannelDao testClass = new CassandraChannelDao(null, null, reader, null);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertFalse(result.isPresent());
	}
}
