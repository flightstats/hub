package com.flightstats.datahub.service.eventing;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubscriptionDispatcher {

	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	private final SubscriptionRoster subscriptions;

	@Inject
	public SubscriptionDispatcher(SubscriptionRoster subscriptions) {
		this.subscriptions = subscriptions;
	}

	public void subscribe(String channelName, EventSink<URI> sink) {
		subscriptions.subscribe(channelName, sink);
	}

	public void unsubscribe(String channelName, EventSink<URI> sink) {
		subscriptions.unsubscribe(channelName, sink);
	}

	public void dispatch(String channelName, final URI uri) {
		Collection<Runnable> notifyJobs = buildNotifyJobs(uri, channelName);
		for (Runnable job : notifyJobs) {
			threadPool.submit(job);
		}
	}

	private Collection<Runnable> buildNotifyJobs(final URI uri, String channelName) {
		Collection<EventSink<URI>> channelSubscribers = subscriptions.getSubscribers(channelName);
		return Collections2.transform(channelSubscribers, new Function<EventSink<URI>, Runnable>() {
			@Override
			public Runnable apply(final EventSink<URI> input) {
				return new SinkRunnable(input, uri);
			}
		});
	}

	private static class SinkRunnable implements Runnable {
		private final EventSink<URI> input;
		private final URI uri;

		public SinkRunnable(EventSink<URI> input, URI uri) {
			this.input = input;
			this.uri = uri;
		}

		@Override
		public void run() {
			input.sink(uri);
		}
	}
}
