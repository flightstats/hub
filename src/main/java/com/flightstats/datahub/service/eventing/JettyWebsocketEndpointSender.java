package com.flightstats.datahub.service.eventing;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.IOException;
import java.net.URI;

class JettyWebsocketEndpointSender implements Consumer<URI> {

	private final RemoteEndpoint remoteEndpoint;
	private final String remoteAddress;

	public JettyWebsocketEndpointSender(String remoteAddress, RemoteEndpoint remoteEndpoint) {
		this.remoteAddress = remoteAddress;
		this.remoteEndpoint = remoteEndpoint;
	}

	@Override
	public void apply(URI uri) {
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

		JettyWebsocketEndpointSender that = (JettyWebsocketEndpointSender) o;

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
