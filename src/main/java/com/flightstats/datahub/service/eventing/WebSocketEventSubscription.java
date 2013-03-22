package com.flightstats.datahub.service.eventing;

import com.google.common.annotations.VisibleForTesting;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class WebSocketEventSubscription {
	private final Consumer<URI> consumer;
	private final BlockingQueue<WebsocketEvent> queue;

	WebSocketEventSubscription(Consumer<URI> consumer) {
		this(consumer, new LinkedBlockingQueue<WebsocketEvent>());
	}

	@VisibleForTesting
	WebSocketEventSubscription(Consumer<URI> consumer, BlockingQueue<WebsocketEvent> queue) {
		this.consumer = consumer;
		this.queue = queue;
	}

	public Consumer<URI> getConsumer() {
		return consumer;
	}

	public BlockingQueue<WebsocketEvent> getQueue() {
		return queue;
	}

	public void consume(URI uri) {
		consumer.apply(uri);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		WebSocketEventSubscription that = (WebSocketEventSubscription) o;

		if (!consumer.equals(that.consumer)) {
			return false;
		}
		if (!queue.equals(that.queue)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = consumer.hashCode();
		result = 31 * result + queue.hashCode();
		return result;
	}
}
