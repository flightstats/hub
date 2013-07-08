package com.flightstats.datahub.service.eventing;

import java.net.URI;
import java.util.Collection;

public interface SubscriptionRoster {

	void subscribe(String channelName, Consumer<URI> consumer);

	void unsubscribe(String channelName, Consumer<URI> subscription);

	int getTotalSubscriberCount();

	Collection<Consumer<URI>> getSubscribers(String channelName);
}
