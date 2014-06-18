package com.flightstats.hub.websocket;

import com.flightstats.hub.service.ChannelLinkBuilder;
import com.flightstats.hub.util.ChannelNameUtils;
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

//todo - gfm - 1/29/14 - change this to use javax.websocket
@WebSocket(maxTextMessageSize = 1024 * 10)    //10k
public class HubWebSocket {

	private final static Logger logger = LoggerFactory.getLogger(HubWebSocket.class);
	private final Runnable afterDisconnectCallback;
	private final ChannelNameUtils channelNameUtils;
	private final WebsocketSubscribers subscriptions;
	private final ChannelLinkBuilder linkBuilder;
	private String remoteAddress;
    //todo - gfm - 1/15/14 - I'm curious about the lifecycle of this
	private WebsocketConsumer endpointSender;
	private String channelName;

	@Inject
	public HubWebSocket(WebsocketSubscribers subscriptions, ChannelNameUtils channelNameUtils, ChannelLinkBuilder linkBuilder) {
		this(subscriptions, channelNameUtils, linkBuilder, new Runnable() {
			@Override
			public void run() {
				//nop
			}
		});
	}

	HubWebSocket(WebsocketSubscribers subscriptions, ChannelNameUtils channelNameUtils, ChannelLinkBuilder linkBuilder,
                 Runnable afterDisconnectCallback) {
		this.linkBuilder = linkBuilder;
		this.afterDisconnectCallback = afterDisconnectCallback;
		this.channelNameUtils = channelNameUtils;
		this.subscriptions = subscriptions;
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		UpgradeRequest upgradeRequest = session.getUpgradeRequest();
		URI requestUri = upgradeRequest.getRequestURI();
        remoteAddress = session.getRemoteAddress().toString();
        logger.info("New client connection: " + remoteAddress + " for " + requestUri);
		String host = upgradeRequest.getHeader("Host");
        channelName = channelNameUtils.extractFromWS(requestUri);
		//this is totally hacky.
        String channelUri = "http://" + host + "/channel/" + channelName;
		try {
			endpointSender = new WebsocketConsumer(remoteAddress, session.getRemote(), linkBuilder, new URI(channelUri));
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
