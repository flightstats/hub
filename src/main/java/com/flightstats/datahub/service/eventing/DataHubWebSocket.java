package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebSocket(maxMessageSize = 1024 * 10)    //10k
public class DataHubWebSocket {

	private final static Logger logger = LoggerFactory.getLogger(DataHubWebSocket.class);

	@OnWebSocketConnect
	public void onConnect(final Session session) {
		logger.info("New client connection: " + session.getRemoteAddress());
		RemoteEndpoint remote = session.getRemote();
		try {
			remote.sendString("Hi client.");
		} catch (IOException e) {
			throw new RuntimeException("Error replying to client: ", e);
		}
	}

	@OnWebSocketClose
	public void onDisconnect(final Session session, int statusCode, String reason) {
		logger.info("Client disconnect: " + session.getRemoteAddress());
	}
}
