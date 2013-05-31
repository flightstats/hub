package com.flightstats.datahub.service.eventing;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.net.URI;
import java.util.concurrent.BlockingQueue;

@Singleton
public class SubscriptionDispatcher {

	private final SubscriptionRoster subscriptions;

	@Inject
	public SubscriptionDispatcher(SubscriptionRoster subscriptions) {
		this.subscriptions = subscriptions;
	}

	public void dispatch(String channelName, URI payloadUri) {
		WebSocketEvent event = new WebSocketEvent(payloadUri);
		for (WebSocketEventSubscription subscriber : subscriptions.getSubscribers(channelName)) {
			BlockingQueue<WebSocketEvent> queue = subscriber.getQueue();
			queue.add(event);
		}
	}

}
