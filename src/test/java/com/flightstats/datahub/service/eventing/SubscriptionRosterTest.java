package com.flightstats.datahub.service.eventing;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

public class SubscriptionRosterTest {

	private EventSink<URI> sink;

	@Before
	public void setup() {
		sink = new NoOpEventSink(42);
	}

	@Test
	public void testSubscribe() throws Exception {
		SubscriptionRoster testClass = new SubscriptionRoster();
		testClass.subscribe("mychan", sink);
		assertThat(testClass.getSubscribers("mychan"), hasItem(sink));
	}

	@Test
	public void testUnsubscribe() throws Exception {
		SubscriptionRoster testClass = new SubscriptionRoster();
		testClass.subscribe("mychan", sink);
		testClass.unsubscribe("mychan", sink);
		assertTrue(testClass.getSubscribers("mychan").isEmpty());
	}

	@Test
	public void testGetSubscribers() throws Exception {
		String channel1 = "mychan";
		String channel2 = "chan2";
		EventSink<URI> sink1 = new NoOpEventSink(1);
		EventSink<URI> sink2 = new NoOpEventSink(2);
		EventSink<URI> sink3 = new NoOpEventSink(3);

		SubscriptionRoster testClass = new SubscriptionRoster();

		testClass.subscribe(channel1, sink1);
		testClass.subscribe(channel2, sink2);
		testClass.subscribe(channel1, sink3);

		assertEquals(2, testClass.getSubscribers(channel1).size());
		assertEquals(1, testClass.getSubscribers(channel2).size());
		assertThat(testClass.getSubscribers(channel1), hasItems(sink1, sink3));
		assertThat(testClass.getSubscribers(channel2), hasItem(sink2));

	}

	private static class NoOpEventSink implements EventSink<URI> {
		private final int id;

		public NoOpEventSink(int id) {
			this.id = id;
		}

		@Override
		public void sink(URI uri) {
			//nop
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			NoOpEventSink that = (NoOpEventSink) o;

			if (id != that.id) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}
}
