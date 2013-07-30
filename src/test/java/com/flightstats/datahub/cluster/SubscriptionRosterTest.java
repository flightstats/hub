package com.flightstats.datahub.cluster;

import com.flightstats.datahub.service.InsertionTopicProxy;
import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SubscriptionRosterTest {

	@Test
	public void testSubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";
		String key = "0000000007H40000";

		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		Message message = mock(Message.class);
		Consumer<String> consumer = mock(Consumer.class);
		InsertionTopicProxy insertionTopicProxy = mock(InsertionTopicProxy.class);

		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		SubscriptionRoster testClass = new SubscriptionRoster(insertionTopicProxy, keyRenderer);

		//WHEN
		when(message.getMessageObject()).thenReturn(key);

		testClass.subscribe(channelName, consumer);

		//THEN
		verify(insertionTopicProxy).subscribe(eq(channelName), messageListenerCaptor.capture());
		messageListenerCaptor.getValue().onMessage(message);
		verify(consumer).apply(key);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";

		DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();
		Consumer<String> consumer = mock(Consumer.class);
		InsertionTopicProxy insertionTopicProxy = mock(InsertionTopicProxy.class);

		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		SubscriptionRoster testClass = new SubscriptionRoster(insertionTopicProxy, keyRenderer);

		//WHEN
		testClass.subscribe(channelName, consumer);        //Need to subscribe first because this class is stateful
		testClass.unsubscribe(channelName, consumer);

		//THEN
		verify(insertionTopicProxy).subscribe(eq(channelName), messageListenerCaptor.capture());
		verify(insertionTopicProxy).unsubscribe(channelName, messageListenerCaptor.getValue());
		assertEquals(0, testClass.getTotalSubscriberCount());
	}
}
