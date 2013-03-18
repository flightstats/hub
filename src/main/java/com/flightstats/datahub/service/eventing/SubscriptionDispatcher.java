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

	public void subscribe(String channelName, Consumer<URI> sink) {
		subscriptions.subscribe(channelName, sink);

	}

	public void unsubscribe(String channelName, Consumer<URI> sink) {
		subscriptions.unsubscribe(channelName, sink);
	}

	public void dispatch(String channelName, final URI uri) {
		Collection<Runnable> notifyJobs = buildNotifyJobs(uri, channelName);
		for (Runnable job : notifyJobs) {
			threadPool.submit(job);
		}
	}

	private Collection<Runnable> buildNotifyJobs(final URI uri, String channelName) {
		Collection<Consumer<URI>> channelSubscribers = subscriptions.getSubscribers(channelName);
		return Collections2.transform(channelSubscribers, new Function<Consumer<URI>, Runnable>() {
			@Override
			public Runnable apply(final Consumer<URI> input) {
				return new RunnableConsumer(input, uri);
			}
		});
	}

	private static class RunnableConsumer implements Runnable {
		private final Consumer<URI> input;
		private final URI uri;

		public RunnableConsumer(Consumer<URI> input, URI uri) {
			this.input = input;
			this.uri = uri;
		}

		@Override
		public void run() {
			input.apply(uri);
		}
	}
}
