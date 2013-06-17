package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class CustomWebSocketCreator implements WebSocketCreator {

	@Override
	public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
		return new DataHubWebSocket();
	}
}
