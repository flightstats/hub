package com.flightstats.datahub.cluster;

import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HazelcastSubscriptionRosterTest {

	@Test
	public void testSubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";
		String key = "0000000007H40000";

		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		ITopic<Object> topic = mock(ITopic.class);
		Message message = mock(Message.class);
		Consumer<String> consumer = mock(Consumer.class);
		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(hazelcast, keyRenderer);

		//WHEN
		when(hazelcast.getTopic("ws:4chan")).thenReturn(topic);
		when(message.getMessageObject()).thenReturn(key);

		testClass.subscribe(channelName, consumer);

		//THEN
		verify(topic).addMessageListener(messageListenerCaptor.capture());
		messageListenerCaptor.getValue().onMessage(message);
		verify(consumer).apply(key);
        assertEquals(Arrays.asList( consumer), testClass.getSubscribers( channelName ) );
	}

	@Test
	public void testUnsubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";

		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		ITopic<Object> topic = mock(ITopic.class);
		Consumer<String> consumer = mock(Consumer.class);
		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		HazelcastSubscriptionRoster testClass = new HazelcastSubscriptionRoster(hazelcast, keyRenderer);

		//WHEN
		when(hazelcast.getTopic("ws:4chan")).thenReturn(topic);

		testClass.subscribe(channelName, consumer);        //Need to subscribe first because this class is stateful
		testClass.unsubscribe(channelName, consumer);

		//THEN
		verify(topic).addMessageListener(messageListenerCaptor.capture());
		verify(topic).removeMessageListener(messageListenerCaptor.getValue());
        assertEquals(0, testClass.getTotalSubscriberCount());
	}
}
