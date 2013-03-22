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
		WebsocketEvent event = new WebsocketEvent(payloadUri);
		for (WebSocketEventSubscription subscriber : subscriptions.getSubscribers(channelName)) {
			BlockingQueue<WebsocketEvent> queue = subscriber.getQueue();
			queue.add(event);
		}
	}

}
