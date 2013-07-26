package com.flightstats.datahub.service.eventing;

import java.util.Collection;

public interface SubscriptionRoster {

	void subscribe(String channelName, Consumer<String> consumer);

	void unsubscribe(String channelName, Consumer<String> subscription);

	int getTotalSubscriberCount();

	Collection<Consumer<String>> getSubscribers(String channelName);
}
