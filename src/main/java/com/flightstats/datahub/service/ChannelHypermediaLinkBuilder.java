package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class ChannelHypermediaLinkBuilder {

	private final UriInfo uriInfo;

	@Inject
	public ChannelHypermediaLinkBuilder(UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}

	URI buildChannelUri(ChannelConfiguration channelConfiguration) {
		URI requestUri = uriInfo.getRequestUri();
		return URI.create(requestUri + "/" + channelConfiguration.getName());
	}

	URI buildLatestUri(ChannelConfiguration channelConfiguration) {
		URI requestUri = uriInfo.getRequestUri();
		return URI.create(requestUri + "/" + channelConfiguration.getName() + "/latest");
	}
}
