package com.flightstats.datahub.cluster;

import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.service.eventing.SingleProcessSubscriptionRoster;
import com.flightstats.datahub.service.eventing.WebSocketEventSubscription;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HazelcastSubscriptionRosterTest {

	@Test
	public void testSubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";
		URI uri = URI.create("http://datahub.flightstats.com/channel/4chan/1234");

		SingleProcessSubscriptionRoster delegate = mock(SingleProcessSubscriptionRoster.class);
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		ITopic<Object> topic = mock(ITopic.class);
		Message message = mock(Message.class);
		Consumer<URI> consumer = mock(Consumer.class);
		WebSocketEventSubscription subscription = mock(WebSocketEventSubscription.class);
		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(delegate, hazelcast);

		//WHEN
		when(hazelcast.getTopic("ws:4chan")).thenReturn(topic);
		when(message.getMessageObject()).thenReturn(uri);
		when(delegate.subscribe(channelName, consumer)).thenReturn(subscription);

		WebSocketEventSubscription result = testClass.subscribe(channelName, consumer);

		//THEN

		verify(topic).addMessageListener(messageListenerCaptor.capture());
		messageListenerCaptor.getValue().onMessage(message);
		verify(consumer).apply(uri);
		verify(delegate).subscribe(channelName, consumer);
		assertEquals(subscription, result);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";
		URI uri = URI.create("http://datahub.flightstats.com/channel/4chan/1234");

		SingleProcessSubscriptionRoster delegate = mock(SingleProcessSubscriptionRoster.class);
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		ITopic<Object> topic = mock(ITopic.class);
		Message message = mock(Message.class);
		Consumer<URI> consumer = mock(Consumer.class);
		WebSocketEventSubscription subscription = mock(WebSocketEventSubscription.class);
		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(delegate, hazelcast);

		//WHEN
		when(hazelcast.getTopic("ws:4chan")).thenReturn(topic);
		when(message.getMessageObject()).thenReturn(uri);
		when(delegate.subscribe(channelName, consumer)).thenReturn(subscription);

		testClass.subscribe(channelName, consumer);        //Need to subscribe first because this class is stateful
		testClass.unsubscribe(channelName, subscription);

		//THEN
		verify(delegate).unsubscribe(channelName, subscription);
		verify(topic).addMessageListener(messageListenerCaptor.capture());
		verify(topic).removeMessageListener(messageListenerCaptor.getValue());
	}

	@Test
	public void testFindSubscriptionForConsumer() throws Exception {
		//GIVEN
		String channelName = "spoon";
		SingleProcessSubscriptionRoster delegate = mock(SingleProcessSubscriptionRoster.class);
		Consumer consumer = mock(Consumer.class);

		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(delegate, null);

		//WHEN
		testClass.findSubscriptionForConsumer(channelName, consumer);

		//THEN

		verify(delegate).findSubscriptionForConsumer(channelName, consumer);
	}

	@Test
	public void testGetTotalSubscriberCount() throws Exception {
		//GIVEN
		SingleProcessSubscriptionRoster delegate = mock(SingleProcessSubscriptionRoster.class);

		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(delegate, null);

		//WHEN
		testClass.getTotalSubscriberCount();

		//THEN
		verify(delegate).getTotalSubscriberCount();
	}

	@Test
	public void testGetSubscribers() throws Exception {
		//GIVEN
		String channelName = "spoon";
		SingleProcessSubscriptionRoster delegate = mock(SingleProcessSubscriptionRoster.class);
		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(delegate, null);

		//WHEN
		testClass.getSubscribers(channelName);

		//THEN
		verify(delegate).getSubscribers(channelName);
	}

}
