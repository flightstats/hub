package com.flightstats.datahub.service.eventing;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.flightstats.datahub.service.ChannelHypermediaLinkBuilder;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.net.URI;

//todo - gfm - 1/15/14 - I don't think this is getting used
public class MetricsCustomWebSocketCreator implements WebSocketCreator {

	private final MetricRegistry registry;
    private final SubscriptionRoster subscriptions;
    private final ChannelNameExtractor channelNameExtractor;
	private final Object mutex = new Object();
	private final ChannelHypermediaLinkBuilder linkBuilder;

	@Inject
	public MetricsCustomWebSocketCreator(MetricRegistry registry, SubscriptionRoster subscriptions, ChannelNameExtractor channelNameExtractor, ChannelHypermediaLinkBuilder linkBuilder) {
		this.registry = registry;
        this.subscriptions = subscriptions;
        this.channelNameExtractor = channelNameExtractor;
		this.linkBuilder = linkBuilder;
	}

	@Override
	public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
		URI requestUri = req.getRequestURI();
		final String channelName = channelNameExtractor.extractFromWS(requestUri);
		final String meterName = "websocket-clients.channels." + channelName;

		synchronized (mutex) {
			registry.counter(meterName).inc();
		}

		return new DataHubWebSocket( subscriptions,  channelNameExtractor, linkBuilder, new Runnable() {
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
