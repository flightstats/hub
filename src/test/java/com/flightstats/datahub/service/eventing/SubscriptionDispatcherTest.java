package com.flightstats.datahub.service.eventing;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SubscriptionDispatcherTest {

	private String channelName;
	private SubscriptionRoster roster;
	private Consumer<URI> sink;

	@Before
	public void setup() {
		channelName = "pseudo";
		roster = mock(SubscriptionRoster.class);
		sink = mock(Consumer.class);
	}

	@Test
	public void testSubscribe() throws Exception {
		SubscriptionDispatcher dispatcher = new SubscriptionDispatcher(roster);

		dispatcher.subscribe(channelName, sink);
		verify(roster).subscribe(channelName, sink);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		SubscriptionDispatcher dispatcher = new SubscriptionDispatcher(roster);

		dispatcher.unsubscribe(channelName, sink);
		verify(roster).unsubscribe(channelName, sink);
	}

	@Test
	public void testDispatch() throws Exception {
		URI uri = URI.create("http://spoon.com");
		SubscriptionDispatcher dispatcher = new SubscriptionDispatcher(new SubscriptionRoster());
		final AtomicReference<URI> sunkUri = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		Consumer<URI> latchSink = new Consumer<URI>() {
			@Override
			public void apply(URI uri) {
				sunkUri.set(uri);
				latch.countDown();
			}
		};
		dispatcher.subscribe(channelName, latchSink);
		dispatcher.dispatch(channelName, uri);

		boolean result = latch.await(5, TimeUnit.SECONDS);
		assertTrue(result);
		assertEquals(uri, sunkUri.get());
	}
}
