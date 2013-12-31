package com.flightstats.datahub.service.eventing;

import com.flightstats.datahub.service.ChannelHypermediaLinkBuilder;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.IOException;
import java.net.URI;

class JettyWebSocketEndpointSender implements Consumer<String> {

	private final RemoteEndpoint remoteEndpoint;
	private final String remoteAddress;
	private final ChannelHypermediaLinkBuilder linkBuilder;
	private final URI channelUri;

	public JettyWebSocketEndpointSender(String remoteAddress, RemoteEndpoint remoteEndpoint, ChannelHypermediaLinkBuilder linkBuilder, URI channelUri) {
		this.remoteAddress = remoteAddress;
		this.remoteEndpoint = remoteEndpoint;
		this.linkBuilder = linkBuilder;
		this.channelUri = channelUri;
	}

	@Override
	public void apply(String stringKey) {
		try {
            URI itemUri = linkBuilder.buildItemUri(stringKey, channelUri);
            remoteEndpoint.sendString(itemUri.toString());
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

		JettyWebSocketEndpointSender that = (JettyWebSocketEndpointSender) o;

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
