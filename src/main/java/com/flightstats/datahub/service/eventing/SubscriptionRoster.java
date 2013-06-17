package com.flightstats.datahub.service.eventing;

import com.google.common.base.Optional;

import java.net.URI;
import java.util.Collection;

public interface SubscriptionRoster {


	WebSocketEventSubscription subscribe(String channelName, Consumer<URI> consumer);

	void unsubscribe(String channelName, WebSocketEventSubscription subscription);

	Optional<WebSocketEventSubscription> findSubscriptionForConsumer(String channelName, Consumer<URI> consumer);

	Integer getTotalSubscriberCount();

	Collection<WebSocketEventSubscription> getSubscribers(String channelName);
}
