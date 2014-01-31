package com.flightstats.hub.service.eventing;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.service.ChannelLinkBuilder;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.net.URI;

public class MetricsCustomWebSocketCreator implements WebSocketCreator {

	private final MetricRegistry registry;
    private final SubscriptionRoster subscriptions;
    private final ChannelNameExtractor channelNameExtractor;
	private final Object mutex = new Object();
	private final ChannelLinkBuilder linkBuilder;

	@Inject
	public MetricsCustomWebSocketCreator(MetricRegistry registry, SubscriptionRoster subscriptions, ChannelNameExtractor channelNameExtractor, ChannelLinkBuilder linkBuilder) {
		this.registry = registry;
        this.subscriptions = subscriptions;
        this.channelNameExtractor = channelNameExtractor;
		this.linkBuilder = linkBuilder;
	}

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        URI requestUri = req.getRequestURI();
        final String channelName = channelNameExtractor.extractFromWS(requestUri);
        final String meterName = "websocket-clients.channels." + channelName;

        synchronized (mutex) {
            registry.counter(meterName).inc();
        }

        return new HubWebSocket( subscriptions,  channelNameExtractor, linkBuilder, new Runnable() {
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
