package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.hazelcast.core.Message;
import org.junit.Test;
import org.mockito.InOrder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import static org.mockito.Mockito.*;

public class HazelcastSubscriberTest {

	@Test
	public void testOneMessageBasic() throws URISyntaxException {
		// GIVEN
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		DataHubKey key = new DataHubKey(new Date(123456L), (short) 0);
		String stringKey = keyRenderer.keyToString(key);
		Consumer<String> consumer = mock(Consumer.class);
		HazelcastSubscriber testClass = new HazelcastSubscriber(consumer, keyRenderer);

		// WHEN
		testClass.onMessage(new Message<>("foo", stringKey));

		// THEN
		verify(consumer).apply(stringKey);
	}

	@Test
	public void testTwoMessageInOrder() throws URISyntaxException {
		// GIVEN
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		DataHubKey key_1 = new DataHubKey(new Date(123456L), (short) 0);
		DataHubKey key_2 = new DataHubKey(new Date(123456L), (short) 1);
		String stringKey1 = keyRenderer.keyToString(key_1);
		String stringKey2 = keyRenderer.keyToString(key_2);
		Consumer<String> consumer = mock(Consumer.class);
		InOrder messageOrder = inOrder(consumer);
		HazelcastSubscriber testClass = new HazelcastSubscriber(consumer, keyRenderer);

		// WHEN
		testClass.onMessage(new Message<>("foo", stringKey1));
		testClass.onMessage(new Message<>("foo", stringKey2));

		// THEN
		messageOrder.verify(consumer).apply(stringKey1);
		messageOrder.verify(consumer).apply(stringKey2);
	}

	@Test
	public void testTwoMessageOutOfOrder() throws URISyntaxException {
		// GIVEN
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		DataHubKey key_1 = new DataHubKey(new Date(123456L), (short) 0);
		DataHubKey key_2 = new DataHubKey(new Date(123456L), (short) 1);
		String stringKey1 = keyRenderer.keyToString(key_1);
		String stringKey2 = keyRenderer.keyToString(key_2);
		Consumer<String> consumer = mock(Consumer.class);
		HazelcastSubscriber testClass = new HazelcastSubscriber(consumer, keyRenderer);

		// WHEN
		testClass.onMessage(new Message<>("foo", stringKey2));
		testClass.onMessage(new Message<>("foo", stringKey1));

		// THEN
		verify(consumer, times(1)).apply(stringKey2);
		verify(consumer, times(0)).apply(stringKey1);
	}

	@Test
	public void testMessagesFromTheFutureAndPastAndRollover() throws URISyntaxException {
		// GIVEN
		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		DataHubKey key_1 = new DataHubKey(new Date(123455L), (short) (Short.MAX_VALUE - 2));
		DataHubKey key_2 = new DataHubKey(new Date(123456L), (short) (Short.MAX_VALUE - 1));
		DataHubKey key_3 = new DataHubKey(new Date(123457L), Short.MAX_VALUE);
		DataHubKey key_4 = new DataHubKey(new Date(123458L), (short) 0);
		DataHubKey key_5 = new DataHubKey(new Date(123459L), (short) 1);
		DataHubKey key_6 = new DataHubKey(new Date(123459L), (short) 5000);
		String key1 = keyRenderer.keyToString(key_1);
		URI uri_1 = new URI("http://mysystem:7898/channel/mychan/" + key1);
		String key2 = keyRenderer.keyToString(key_2);
		URI uri_2 = new URI("http://mysystem:7898/channel/mychan/" + key2);
		String key3 = keyRenderer.keyToString(key_3);
		URI uri_3 = new URI("http://mysystem:7898/channel/mychan/" + key3);
		String key4 = keyRenderer.keyToString(key_4);
		URI uri_4 = new URI("http://mysystem:7898/channel/mychan/" + key4);
		String key5 = keyRenderer.keyToString(key_5);
		URI uri_5 = new URI("http://mysystem:7898/channel/mychan/" + key5);
		String key6 = keyRenderer.keyToString(key_6);
		URI uri_6 = new URI("http://mysystem:7898/channel/mychan/" + key6);
		Consumer<String> consumer = mock(Consumer.class);
		InOrder messageOrder = inOrder(consumer);
		HazelcastSubscriber testClass = new HazelcastSubscriber(consumer, keyRenderer);

		// WHEN
		testClass.onMessage(new Message<>("foo", key2)); // first message
		testClass.onMessage(new Message<>("foo", key1)); // old and thrown out, lower sequence than uri_2
		testClass.onMessage(new Message<>("foo", key4)); // future
		testClass.onMessage(new Message<>("foo", key5)); // future
		testClass.onMessage(new Message<>("foo", key3)); // expected, then futures should get handled
		testClass.onMessage(new Message<>("foo", key6)); // old and thrown out, due to beyond buffer

		// THEN
		messageOrder.verify(consumer).apply(key2);
		messageOrder.verify(consumer).apply(key3);
		messageOrder.verify(consumer).apply(key4);
		messageOrder.verify(consumer).apply(key5);
		verify(consumer, times(4)).apply(anyString());
	}
}
