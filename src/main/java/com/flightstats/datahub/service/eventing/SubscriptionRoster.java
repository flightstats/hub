package com.flightstats.datahub.service.eventing;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.net.URI;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class SubscriptionRoster {

	private final Multimap<String, WebSocketEventSubscription> channelSubscribers =
			Multimaps.synchronizedMultimap(
					Multimaps.newSetMultimap(new HashMap<String, Collection<WebSocketEventSubscription>>(),
							new Supplier<Set<WebSocketEventSubscription>>() {
								@Override
								public Set<WebSocketEventSubscription> get() {
									return new HashSet<>();
								}
							}));

	public WebSocketEventSubscription subscribe(String channelName, Consumer<URI> consumer) {
		WebSocketEventSubscription subscription = new WebSocketEventSubscription(consumer);
		channelSubscribers.put(channelName, subscription);
		return subscription;
	}

	public void unsubscribe(String channelName, WebSocketEventSubscription subscription) {
		channelSubscribers.remove(channelName, subscription);
	}

	Optional<WebSocketEventSubscription> findSubscriptionForConsumer(String channelName, Consumer<URI> consumer) {
		for (WebSocketEventSubscription consumerEntry : getSubscribers(channelName)) {
			if (consumerEntry.getConsumer().equals(consumer)) {
				return Optional.of(consumerEntry);
			}
		}
		return Optional.absent();
	}

	public Collection<WebSocketEventSubscription> getSubscribers(String channelName) {
		Collection<WebSocketEventSubscription> subscriber = channelSubscribers.get(channelName);
		List<WebSocketEventSubscription> subscribersCopy = newArrayList(subscriber);
		return Collections.unmodifiableCollection(subscribersCopy);
	}

	public Integer getTotalSubscriberCount()
	{
		return channelSubscribers.size();
	}
}
