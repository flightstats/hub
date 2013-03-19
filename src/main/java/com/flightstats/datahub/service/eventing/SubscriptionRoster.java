package com.flightstats.datahub.service.eventing;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.net.URI;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class SubscriptionRoster {

	Multimap<String, Consumer<URI>> subscribers =
			Multimaps.synchronizedSetMultimap(
					Multimaps.newSetMultimap(new HashMap<String, Collection<Consumer<URI>>>(), new Supplier<Set<Consumer<URI>>>() {
						@Override
						public Set<Consumer<URI>> get() {
							return new HashSet<>();
						}
					}));

	public void subscribe(String channelName, Consumer<URI> sink) {
		subscribers.put(channelName, sink);
	}

	public void unsubscribe(String channelName, Consumer<URI> sink) {
		subscribers.remove(channelName, sink);
	}

	public Collection<Consumer<URI>> getSubscribers(String channelName) {
		List<Consumer<URI>> subscribersCopy = newArrayList(subscribers.get(channelName));
		return Collections.unmodifiableCollection(subscribersCopy);
	}
}
