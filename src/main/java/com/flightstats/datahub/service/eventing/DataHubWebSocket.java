package com.flightstats.datahub.service.eventing;

import com.google.common.base.Optional;
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
	private final SubscriptionRoster subscriptions;
	private String remoteAddress;
	private String channelName;
	private JettyWebsocketEndpointSender endpointSender;

	@Inject
	public DataHubWebSocket(SubscriptionRoster subscriptions) {
		this.subscriptions = subscriptions;
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		URI requestUri = session.getUpgradeRequest().getRequestURI();
		logger.info("New client connection: " + remoteAddress + " for " + requestUri);

		remoteAddress = session.getRemoteAddress().toString();
		channelName = extractChannelName(session);
		endpointSender = new JettyWebsocketEndpointSender(remoteAddress, session.getRemote());
		WebSocketEventSubscription subscription = subscriptions.subscribe(channelName, endpointSender);
		new Thread(new SubscriptionDispatchWorker(subscription)).start();
	}

	@OnWebSocketClose
	public void onDisconnect(int statusCode, String reason) {
		logger.info("Client disconnect: " + remoteAddress + " (status = " + statusCode + ", reason = " + reason + ")");
		Optional<WebSocketEventSubscription> optionalSubscription = subscriptions.findSubscriptionForConsumer(channelName, endpointSender);
		if (!optionalSubscription.isPresent()) {
			logger.warn("Cannot unsubscribe:  No subscription on channel " + channelName + " for " + endpointSender);
			return;
		}
		WebSocketEventSubscription subscription = optionalSubscription.get();
		subscription.getQueue().add(WebsocketEvent.SHUTDOWN);
		subscriptions.unsubscribe(channelName, subscription);
	}

	private String extractChannelName(Session session) {
		URI requestURI = session.getUpgradeRequest().getRequestURI();
		String path = requestURI.getPath();
		return path.replaceFirst("^/channel/(.*)/ws$", "$1");
	}

}
