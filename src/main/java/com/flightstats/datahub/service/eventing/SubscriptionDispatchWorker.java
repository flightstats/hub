package com.flightstats.datahub.service.eventing;

import java.util.concurrent.BlockingQueue;

import static com.flightstats.datahub.service.eventing.WebsocketEvent.SHUTDOWN;

class SubscriptionDispatchWorker implements Runnable {

	private final WebSocketEventSubscription subscriber;

	public SubscriptionDispatchWorker(WebSocketEventSubscription subscriber) {
		this.subscriber = subscriber;
	}

	@Override
	public void run() {
		BlockingQueue<WebsocketEvent> queue = subscriber.getQueue();
		while (true) {
			try {
				WebsocketEvent event = queue.take();
				if (SHUTDOWN.equals(event)) {
					break;
				}
				subscriber.consume(event.getUri());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}

		}
	}
}
