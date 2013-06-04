package com.flightstats.datahub.service.eventing;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.net.URI;

public class MetricsCustomWebSocketCreator implements WebSocketCreator {

	private final MetricRegistry registry;
	private final SubscriptionRoster subscriptions;
	private final WebSocketChannelNameExtractor channelNameExtractor;
	private final Object mutex = new Object();

	@Inject
	public MetricsCustomWebSocketCreator(MetricRegistry registry, SubscriptionRoster subscriptions, WebSocketChannelNameExtractor channelNameExtractor) {
		this.registry = registry;
		this.subscriptions = subscriptions;
		this.channelNameExtractor = channelNameExtractor;
	}

	@Override
	public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
		URI requestUri = req.getRequestURI();
		final String channelName = channelNameExtractor.extractChannelName(requestUri);
		final String meterName = "websocket-clients.channels." + channelName;

		synchronized (mutex) {
			registry.counter(meterName).inc();
		}

		return new DataHubWebSocket(subscriptions, channelNameExtractor, new Runnable() {
			@Override
			public void run() {
				synchronized (mutex) {
					Counter counter = registry.counter(meterName);
					counter.dec();
					if (counter.getCount() == 0) {    //Last remaining client for this channel
						registry.remove(meterName);
					}
				}
			}
		});
	}
}
