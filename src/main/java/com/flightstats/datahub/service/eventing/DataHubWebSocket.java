package com.flightstats.datahub.service.eventing;

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
	private final Runnable afterDisconnectCallback;
	private String remoteAddress;

	public DataHubWebSocket() {
		this(new Runnable() {
			@Override
			public void run() {
				//nop
			}
		});
	}

	public DataHubWebSocket(Runnable afterDisconnectCallback) {
		this.afterDisconnectCallback = afterDisconnectCallback;
	}

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		URI requestUri = session.getUpgradeRequest().getRequestURI();
		logger.info("New client connection: " + remoteAddress + " for " + requestUri);

		remoteAddress = session.getRemoteAddress().toString();
	}

	@OnWebSocketClose
	public void onDisconnect(int statusCode, String reason) {
		logger.info("Client disconnect: " + remoteAddress + " (status = " + statusCode + ", reason = " + reason + ")");
		afterDisconnectCallback.run();
	}
}
