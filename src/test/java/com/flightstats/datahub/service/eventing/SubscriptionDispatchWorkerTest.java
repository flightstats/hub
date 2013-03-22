package com.flightstats.datahub.service.eventing;

import org.junit.Test;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;

import static junit.framework.TestCase.assertFalse;
import static org.mockito.Mockito.*;

public class SubscriptionDispatchWorkerTest {

	@Test
	public void testLifecycle() throws Exception {
		LinkedBlockingQueue<WebsocketEvent> queue = new LinkedBlockingQueue<>();
		WebSocketEventSubscription subscriber = mock(WebSocketEventSubscription.class);

		when(subscriber.getQueue()).thenReturn(queue);

		SubscriptionDispatchWorker testClass = new SubscriptionDispatchWorker(subscriber);
		Thread thread = new Thread(testClass);
		thread.start();
		WebsocketEvent e1 = new WebsocketEvent(URI.create("http://path/to/foo1"));
		WebsocketEvent e2 = new WebsocketEvent(URI.create("http://path/to/foo2"));
		queue.add(e1);
		queue.add(e2);
		queue.add(WebsocketEvent.SHUTDOWN);

		thread.join(50);
		assertFalse(thread.isAlive());

		verify(subscriber).consume(e1.getUri());
		verify(subscriber).consume(e2.getUri());

	}
}
