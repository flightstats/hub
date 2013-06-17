package com.flightstats.datahub.service.eventing;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.net.URI;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class SingleProcessSubscriptionRoster implements SubscriptionRoster {

	private final Multimap<String, WebSocketEventSubscription> channelSubscribers =
			Multimaps.synchronizedMultimap(
					Multimaps.newSetMultimap(new HashMap<String, Collection<WebSocketEventSubscription>>(),
							new Supplier<Set<WebSocketEventSubscription>>() {
								@Override
								public Set<WebSocketEventSubscription> get() {
									return new HashSet<>();
								}
							}));

	@Override
	public WebSocketEventSubscription subscribe(String channelName, Consumer<URI> consumer) {
		WebSocketEventSubscription subscription = new WebSocketEventSubscription(consumer);
		channelSubscribers.put(channelName, subscription);
		return subscription;
	}

	@Override
	public void unsubscribe(String channelName, WebSocketEventSubscription subscription) {
		channelSubscribers.remove(channelName, subscription);
	}

	@Override
	public Optional<WebSocketEventSubscription> findSubscriptionForConsumer(String channelName, Consumer<URI> consumer) {
		for (WebSocketEventSubscription consumerEntry : getSubscribers(channelName)) {
			if (consumerEntry.getConsumer().equals(consumer)) {
				return Optional.of(consumerEntry);
			}
		}
		return Optional.absent();
	}

	@Override
	public Collection<WebSocketEventSubscription> getSubscribers(String channelName) {
		Collection<WebSocketEventSubscription> subscriber = channelSubscribers.get(channelName);
		List<WebSocketEventSubscription> subscribersCopy = newArrayList(subscriber);
		return Collections.unmodifiableCollection(subscribersCopy);
	}

	@Override
	public Integer getTotalSubscriberCount() {
		return channelSubscribers.size();
	}

}
