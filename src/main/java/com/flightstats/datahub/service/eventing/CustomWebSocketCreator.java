package com.flightstats.datahub.service.eventing;

import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class CustomWebSocketCreator implements WebSocketCreator {
	private final SubscriptionRoster subscriptions;
	private final WebSocketChannelNameExtractor channelNameExtractor;

	@Inject
	public CustomWebSocketCreator(SubscriptionRoster subscriptions, WebSocketChannelNameExtractor channelNameExtractor) {
		this.subscriptions = subscriptions;
		this.channelNameExtractor = channelNameExtractor;
	}

	@Override
	public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
		return new DataHubWebSocket(subscriptions, channelNameExtractor);
	}
}
