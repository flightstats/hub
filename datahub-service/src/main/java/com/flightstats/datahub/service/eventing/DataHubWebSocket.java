package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.service.ChannelLinkBuilder;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

@WebSocket(maxMessageSize = 1024 * 10)    //10k
public class DataHubWebSocket {

	private final static Logger logger = LoggerFactory.getLogger(DataHubWebSocket.class);
	private final Runnable afterDisconnectCallback;
	private final ChannelNameExtractor channelNameExtractor;
	private final SubscriptionRoster subscriptions;
	private final ChannelLinkBuilder linkBuilder;
	private String remoteAddress;
    //todo - gfm - 1/15/14 - I'm curious about the lifecycle of this
	private JettyWebSocketEndpointSender endpointSender;
	private String channelName;

	@Inject
	public DataHubWebSocket(SubscriptionRoster subscriptions, ChannelNameExtractor channelNameExtractor, ChannelLinkBuilder linkBuilder) {
		this(subscriptions, channelNameExtractor, linkBuilder, new Runnable() {
			@Override
			public void run() {
				//nop
			}
		});
	}

	DataHubWebSocket(SubscriptionRoster subscriptions, ChannelNameExtractor channelNameExtractor, ChannelLinkBuilder linkBuilder,
					Runnable afterDisconnectCallback) {
		this.linkBuilder = linkBuilder;
		this.afterDisconnectCallback = afterDisconnectCallback;
		this.channelNameExtractor = channelNameExtractor;
		this.subscriptions = subscriptions;
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		UpgradeRequest upgradeRequest = session.getUpgradeRequest();
		URI requestUri = upgradeRequest.getRequestURI();
        remoteAddress = session.getRemoteAddress().toString();
        logger.info("New client connection: " + remoteAddress + " for " + requestUri);
		String host = upgradeRequest.getHeader("Host");
        channelName = channelNameExtractor.extractFromWS(requestUri);
		//this is totally hacky.
        String channelUri = "http://" + host + "/channel/" + channelName;
		try {
			endpointSender = new JettyWebSocketEndpointSender(remoteAddress, session.getRemote(), linkBuilder, new URI(channelUri));
		} catch (URISyntaxException e) {
			//this should really never happen.  stupid checked exceptions!
			throw new RuntimeException(e);
		}
		subscriptions.subscribe(channelName, endpointSender);
	}

	@OnWebSocketClose
	public void onDisconnect(int statusCode, String reason) {
		logger.info("Client disconnect: " + remoteAddress + " (status = " + statusCode + ", reason = " + reason + ")");
		afterDisconnectCallback.run();
		subscriptions.unsubscribe(channelName, endpointSender);
	}
}
