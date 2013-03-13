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
	private String remoteAddress;
	private String channelName;
	private JettyWebsocketEndpointSender endpointSender;

	@Inject
	public DataHubWebSocket(SubscriptionDispatcher subscriptionDispatcher) {
		this.subscriptionDispatcher = subscriptionDispatcher;
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		remoteAddress = session.getRemoteAddress().toString();
		channelName = extractChanelName(session);

		URI requestUri = session.getUpgradeRequest().getRequestURI();
		logger.info("New client connection: " + remoteAddress + " for " + requestUri);

		endpointSender = new JettyWebsocketEndpointSender(remoteAddress, session.getRemote());
		subscriptionDispatcher.subscribe(channelName, endpointSender);
	}

	@OnWebSocketClose
	public void onDisconnect(int statusCode, String reason) {
		logger.info("Client disconnect: " + remoteAddress + " (status = " + statusCode + ", reason = " + reason + ")");
		subscriptionDispatcher.unsubscribe(channelName, endpointSender);
	}

	private String extractChanelName(Session session) {
		URI requestURI = session.getUpgradeRequest().getRequestURI();
		String path = requestURI.getPath();
		return path.replaceFirst("^/channel/(.*)/ws$", "$1");
	}

}
