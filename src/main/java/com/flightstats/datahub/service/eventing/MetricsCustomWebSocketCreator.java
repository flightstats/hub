package com.flightstats.datahub.service.eventing;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.net.URI;

public class MetricsCustomWebSocketCreator implements WebSocketCreator {

	private final MetricRegistry registry;
	private final SubscriptionRoster subscriptions;
	private final WebSocketChannelNameExtractor channelNameExtractor;
	private final AtomicLongMap<String> channelSubscriberCounts = AtomicLongMap.create();

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

		synchronized (channelSubscriberCounts) {
			long count = channelSubscriberCounts.incrementAndGet(channelName);
			if (count == 1) {    //First client registering for this channel, time to create a gauge
				registry.register(meterName, new Gauge<Integer>() {
					@Override
					public Integer getValue() {
						return (int) channelSubscriberCounts.get(channelName);
					}
				});
			}
		}
		return new DataHubWebSocket(subscriptions, channelNameExtractor, new Runnable() {
			@Override
			public void run() {
				synchronized (channelSubscriberCounts) {
					if (channelSubscriberCounts.decrementAndGet(channelName) == 0) {    //Last remaining client for this channel
						registry.remove(meterName);
					}
				}
			}
		});
	}
}
