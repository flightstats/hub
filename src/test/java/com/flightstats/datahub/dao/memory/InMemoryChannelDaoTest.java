package com.flightstats.datahub.dao.memory;

import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InMemoryChannelDaoTest {

	@Test
	public void testChannelExists() throws Exception {
		InMemoryChannelDao testClass = new InMemoryChannelDao(mock(TimeProvider.class));
		testClass.createChannel("thechan", null);
		assertTrue(testClass.channelExists("thechan"));
		assertFalse(testClass.channelExists("nope"));
	}

	@Test
	public void testCreateChannel() throws Exception {
		Date creationDate = new Date(9999);
		ChannelConfiguration expected = new ChannelConfiguration("foo", creationDate, null);
		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(creationDate);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		ChannelConfiguration result = testClass.createChannel("foo", null);
		assertEquals(expected, result);
	}

	@Test
	public void testGetChannelConfiguration() throws Exception {
		Date creationDate = new Date(9999);
		ChannelConfiguration expected = new ChannelConfiguration("foo", creationDate, null);
		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(creationDate);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel("foo", null);
		assertEquals(expected, testClass.getChannelConfiguration("foo"));
	}

	@Test
	public void testCountChannels() throws Exception {
		TimeProvider timeProvider = mock(TimeProvider.class);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel("one", null);
		assertEquals(1, testClass.countChannels());
		testClass.createChannel("two", null);
		assertEquals(2, testClass.countChannels());
	}

	@Test
	public void testFindFirstId() throws Exception {
		InMemoryChannelDao testClass = new InMemoryChannelDao(new TimeProvider());
		testClass.createChannel("channelName", null);
		assertFalse(testClass.findFirstUpdateKey("channelName").isPresent());
		ValueInsertionResult insertionResult = testClass.insert("channelName", Optional.of("text/plain"), null, null, "Hello".getBytes());
		assertEquals(insertionResult.getKey(), testClass.findFirstUpdateKey("channelName").get());
	}

	@Test
	public void testFindLatestId() throws Exception {
		InMemoryChannelDao testClass = new InMemoryChannelDao(new TimeProvider());
		testClass.createChannel("channelName", null);
		assertFalse(testClass.findLastUpdatedKey("channelName").isPresent());
		ValueInsertionResult insertionResult = testClass.insert("channelName", Optional.of("text/plain"), null, null, "Hello".getBytes());
		assertEquals(insertionResult.getKey(), testClass.findLastUpdatedKey("channelName").get());
	}

	@Test
	public void testInsert() throws Exception {
		Date date = new Date(2345678910L);
		DataHubKey key = new DataHubKey(date, (short) 0);
		String channelName = "foo";
		byte[] data = "bar".getBytes();
		Optional<String> contentType = Optional.of("text/plain");
		ValueInsertionResult expected = new ValueInsertionResult(key);

		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(date);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel(channelName, null);

		ValueInsertionResult result = testClass.insert(channelName, contentType, null, null, data);

		assertEquals(expected, result);
	}

	@Test
	public void testGetValue_previousAndNext() throws Exception {
		String channelName = "cccccc";
		Date channelCreationDate = new Date();
		Date previousDate = new Date(9998888777665L);
		Date date = new Date(9998888777666L);
		Date nextDate = new Date(9998888777667L);

		DataHubKey previousKey = new DataHubKey(previousDate, (short) 0);
		DataHubKey key = new DataHubKey(date, (short) 1);
		DataHubKey nextKey = new DataHubKey(nextDate, (short) 2);
		byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};

		Optional<DataHubKey> previous = Optional.of(previousKey);
		Optional<DataHubKey> next = Optional.of(nextKey);
		LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(new DataHubCompositeValue(Optional.of("text/plain"), null, null, data), previous, next);

		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(channelCreationDate, previousDate, date, nextDate);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel(channelName, null);
		testClass.insert(channelName, Optional.of("text/plain"), null, null, "foo".getBytes());
		testClass.insert(channelName, Optional.of("text/plain"), null, null, data);
		testClass.insert(channelName, Optional.of("text/plain"), null, null, "bar".getBytes());

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertEquals(expected, result.get());
	}

	@Test
	public void testGetValue_previous() throws Exception {
		String channelName = "cccccc";
		Date channelCreationDate = new Date();
		Date previousDate = new Date(9998888777665L);
		Date date = new Date(9998888777666L);

		DataHubKey previousKey = new DataHubKey(previousDate, (short) 0);
		DataHubKey key = new DataHubKey(date, (short) 1);
		byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};

		Optional<DataHubKey> previous = Optional.of(previousKey);
		LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(new DataHubCompositeValue(Optional.of("text/plain"), null, null, data), previous,
				Optional.<DataHubKey>absent());

		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(channelCreationDate, previousDate, date);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel(channelName, null);
		testClass.insert(channelName, Optional.of("text/plain"), null, null, "foo".getBytes());
		testClass.insert(channelName, Optional.of("text/plain"), null, null, data);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertEquals(expected, result.get());
	}

	@Test
	public void testGetValue() throws Exception {
		String channelName = "cccccc";
		Date channelCreationDate = new Date();
		Date date = new Date(9998888777666L);

		byte[] data = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};

		LinkedDataHubCompositeValue expected = new LinkedDataHubCompositeValue(new DataHubCompositeValue(Optional.of("text/plain"), null,
				null, data),
				Optional.<DataHubKey>absent(), Optional.<DataHubKey>absent());

		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(channelCreationDate, date);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel(channelName, null);
		ValueInsertionResult valueInsertionResult = testClass.insert(channelName, Optional.of("text/plain"), null, null, data);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, valueInsertionResult.getKey());
		assertEquals(expected, result.get());
	}

	@Test
	public void testGetValue_crossChannel() throws Exception {
		String channelName = "cccccc";
		Date channelCreationDate = new Date();
		Date date = new Date(9998888777666L);

		byte[] data = "hello".getBytes();

		TimeProvider timeProvider = mock(TimeProvider.class);
		when(timeProvider.getDate()).thenReturn(channelCreationDate, date);

		InMemoryChannelDao testClass = new InMemoryChannelDao(timeProvider);
		testClass.createChannel(channelName, null);
		ValueInsertionResult valueInsertionResult = testClass.insert(channelName, Optional.of("text/plain"), null, null, data);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue("otherChannel", valueInsertionResult.getKey());
		assertFalse(result.isPresent());
	}

	@Test
	public void testGetValue_notFound() throws Exception {
		String channelName = "cccccc";
		DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);

		InMemoryChannelDao testClass = new InMemoryChannelDao(mock(TimeProvider.class));

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue(channelName, key);
		assertFalse(result.isPresent());
	}

}
