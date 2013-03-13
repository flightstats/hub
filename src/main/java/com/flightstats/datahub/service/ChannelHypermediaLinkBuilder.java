package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.inject.Inject;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class ChannelHypermediaLinkBuilder {

	private final UriInfo uriInfo;
	private final DataHubKeyRenderer keyRenderer;

	@Inject
	public ChannelHypermediaLinkBuilder(UriInfo uriInfo, DataHubKeyRenderer keyRenderer) {
		this.uriInfo = uriInfo;
		this.keyRenderer = keyRenderer;
	}

	URI buildChannelUri(ChannelConfiguration channelConfiguration) {
		return buildChannelUri(channelConfiguration.getName());
	}

	URI buildChannelUri(String channelName) {
		URI requestUri = uriInfo.getRequestUri();
		return URI.create(requestUri + "/" + channelName);
	}

	URI buildLatestUri(ChannelConfiguration channelConfiguration) {
		URI requestUri = uriInfo.getRequestUri();
		return URI.create(requestUri + "/" + channelConfiguration.getName() + "/latest");
	}

	public URI buildItemUri(DataHubKey key) {
		URI channelUri = uriInfo.getRequestUri();
		String keyId = keyRenderer.keyToString(key);
		return URI.create(channelUri.toString() + "/" + keyId);
	}
}
