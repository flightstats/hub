package com.flightstats.hub.websocket;

import com.flightstats.hub.service.ChannelLinkBuilder;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.IOException;
import java.net.URI;

class WebsocketConsumer implements Consumer<String> {

	private final RemoteEndpoint remoteEndpoint;
	private final String remoteAddress;
	private final ChannelLinkBuilder linkBuilder;
	private final URI channelUri;

	public WebsocketConsumer(String remoteAddress, RemoteEndpoint remoteEndpoint, ChannelLinkBuilder linkBuilder, URI channelUri) {
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

		WebsocketConsumer that = (WebsocketConsumer) o;

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
