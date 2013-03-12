package com.flightstats.datahub.service.eventing;

import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@WebSocket(maxMessageSize = 1024 * 10)    //10k
public class DataHubWebSocket {

	private final static Logger logger = LoggerFactory.getLogger(DataHubWebSocket.class);
	private final SubscriptionDispatcher subscriptionDispatcher;

	@Inject
	public DataHubWebSocket(SubscriptionDispatcher subscriptionDispatcher) {
		this.subscriptionDispatcher = subscriptionDispatcher;
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		URI requestUri = session.getUpgradeRequest().getRequestURI();
		String channelName = extractChanelName(session);
		logger.info("New client connection: " + session.getRemoteAddress() + " for " + requestUri);
		subscriptionDispatcher.subscribe(channelName, new JettyWebsocketEndpointSender(session.getRemoteAddress().toString(), session.getRemote()));
	}

	@OnWebSocketClose
	public void onDisconnect(final Session session, int statusCode, String reason) {
		logger.info("Client disconnect: " + session.getRemoteAddress() + " (" + reason + ")");
		String channelName = extractChanelName(session);
		subscriptionDispatcher.unsubscribe(channelName, new JettyWebsocketEndpointSender(session.getRemoteAddress().toString(), session.getRemote()));
	}

	private String extractChanelName(Session session) {
		URI requestURI = session.getUpgradeRequest().getRequestURI();
		String path = requestURI.getPath();
		return path.replaceFirst("^/channel/(.*)/ws$", "$1");
	}

}
