package com.flightstats.datahub.service.eventing;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.net.URI;
import java.util.*;

public class SubscriptionRoster {

	Multimap<String, EventSink<URI>> subscribers =
			Multimaps.synchronizedSetMultimap(
					Multimaps.newSetMultimap(new HashMap<String, Collection<EventSink<URI>>>(), new Supplier<Set<EventSink<URI>>>() {
						@Override
						public Set<EventSink<URI>> get() {
							return new HashSet<>();
						}
					}));

	public void subscribe(String channelName, EventSink<URI> sink) {
		subscribers.put(channelName, sink);
	}

	public void unsubscribe(String channelName, EventSink<URI> sink) {
		subscribers.remove(channelName, sink);
	}

	public Collection<EventSink<URI>> getSubscribers(String channelName) {
		return Collections.unmodifiableCollection(subscribers.get(channelName));
	}
}
