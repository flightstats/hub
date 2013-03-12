package com.flightstats.datahub.service.eventing;

import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
		subscriptionDispatcher.subscribe(channelName, new URIEventSink(session.getRemoteAddress().toString(), session.getRemote()));
	}

	@OnWebSocketClose
	public void onDisconnect(final Session session, int statusCode, String reason) {
		logger.info("Client disconnect: " + session.getRemoteAddress() + " (" + reason + ")");
		String channelName = extractChanelName(session);
		subscriptionDispatcher.unsubscribe(channelName, new URIEventSink(session.getRemoteAddress().toString(), session.getRemote()));
	}

	private String extractChanelName(Session session) {
		URI requestURI = session.getUpgradeRequest().getRequestURI();
		String path = requestURI.getPath();
		return path.replaceFirst("^/channel/(.*)/ws$", "$1");
	}

	private static class URIEventSink implements EventSink<URI> {
		private final RemoteEndpoint remoteEndpoint;
		private final String remoteAddress;

		public URIEventSink(String remoteAddress, RemoteEndpoint remoteEndpoint) {
			this.remoteAddress = remoteAddress;
			this.remoteEndpoint = remoteEndpoint;
		}

		@Override
		public void sink(URI uri) {
			try {
				remoteEndpoint.sendString(uri.toString());
			} catch (IOException e) {
				throw new RuntimeException("Error replying to client: ", e);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			URIEventSink that = (URIEventSink) o;

			if (!remoteAddress.equals(that.remoteAddress)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return remoteAddress.hashCode();
		}
	}
}
