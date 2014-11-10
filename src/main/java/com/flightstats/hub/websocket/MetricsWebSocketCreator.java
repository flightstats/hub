package com.flightstats.hub.websocket;

import com.flightstats.hub.service.ChannelLinkBuilder;
import com.flightstats.hub.util.ChannelNameUtils;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class MetricsWebSocketCreator implements WebSocketCreator {

    private final WebsocketSubscribers subscriptions;
    private final ChannelNameUtils channelNameUtils;
	private final ChannelLinkBuilder linkBuilder;

	@Inject
    public MetricsWebSocketCreator(WebsocketSubscribers subscriptions, ChannelNameUtils channelNameUtils, ChannelLinkBuilder linkBuilder) {
        this.subscriptions = subscriptions;
        this.channelNameUtils = channelNameUtils;
		this.linkBuilder = linkBuilder;
	}

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return new HubWebSocket(subscriptions, channelNameUtils, linkBuilder);
    }
}
