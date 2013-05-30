package com.flightstats.datahub.service.eventing;

import java.util.concurrent.BlockingQueue;

class SubscriptionDispatchWorker implements Runnable {

	private final WebSocketEventSubscription subscriber;

	public SubscriptionDispatchWorker(WebSocketEventSubscription subscriber) {
		this.subscriber = subscriber;
	}

	@Override
	public void run() {
		BlockingQueue<WebSocketEvent> queue = subscriber.getQueue();
		while (true) {
			try {
				WebSocketEvent event = queue.take();
				if (event.isShutdown()) {
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
