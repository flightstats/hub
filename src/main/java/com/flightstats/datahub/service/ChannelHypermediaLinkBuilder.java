package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class ChannelHypermediaLinkBuilder {

	private final URI requestUri;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public ChannelHypermediaLinkBuilder(UriInfo uriInfo, DataHubKeyRenderer keyRenderer) {
		this.requestUri = uriInfo.getRequestUri();
		this.keyRenderer = keyRenderer;
	}

	URI buildChannelUri(ChannelConfiguration channelConfiguration) {
		return buildChannelUri(channelConfiguration.getName());
	}

	URI buildChannelUri(String channelName) {
		return URI.create(requestUri + "/" + channelName);
	}

	URI buildLatestUri(ChannelConfiguration channelConfiguration) {
		return URI.create(requestUri + "/" + channelConfiguration.getName() + "/latest");
	}

	public URI buildItemUri(DataHubKey key) {
		String keyId = keyRenderer.keyToString(key);
		return URI.create(requestUri.toString() + "/" + keyId);
	}

	public URI buildWsLinkFor(ChannelConfiguration channelConfiguration) {
		String requestUri = this.requestUri.toString().replaceFirst("^http", "ws");
		return URI.create(requestUri + "/" + channelConfiguration.getName() + "/ws");
	}
}
