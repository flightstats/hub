package com.flightstats.datahub.service.eventing;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import static org.mockito.Mockito.*;

public class SubscriptionDispatcherTest {

	@Test
	public void testDispatch() throws Exception {
		URI uri = URI.create("http://spoon.com");
		WebSocketEvent event = new WebSocketEvent(uri);

		SingleProcessSubscriptionRoster roster = mock(SingleProcessSubscriptionRoster.class);
		WebSocketEventSubscription sub1 = mock(WebSocketEventSubscription.class);
		WebSocketEventSubscription sub2 = mock(WebSocketEventSubscription.class);
		WebSocketEventSubscription sub3 = mock(WebSocketEventSubscription.class);

		BlockingQueue<WebSocketEvent> queue1 = mock(BlockingQueue.class);
		BlockingQueue<WebSocketEvent> queue2 = mock(BlockingQueue.class);
		BlockingQueue<WebSocketEvent> queue3 = mock(BlockingQueue.class);

		when(sub1.getQueue()).thenReturn(queue1);
		when(sub2.getQueue()).thenReturn(queue2);
		when(sub3.getQueue()).thenReturn(queue3);

		when(roster.getSubscribers("chan1")).thenReturn(Arrays.asList(sub1, sub2, sub3));

		SubscriptionDispatcher testClass = new SubscriptionDispatcher(roster);

		testClass.dispatch("chan1", uri);

		verify(queue1).add(event);
		verify(queue2).add(event);
		verify(queue3).add(event);

	}
}
