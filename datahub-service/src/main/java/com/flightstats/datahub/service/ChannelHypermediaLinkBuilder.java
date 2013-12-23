package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

public class ChannelHypermediaLinkBuilder {

	@Inject
	public ChannelHypermediaLinkBuilder() {
	}

	URI buildChannelUri(ChannelConfiguration channelConfiguration, UriInfo uriInfo) {
		return buildChannelUri(channelConfiguration.getName(), uriInfo);
	}

	URI buildChannelUri(String channelName, UriInfo uriInfo) {
		return URI.create(uriInfo.getBaseUri() + "channel/" + channelName);
	}

	URI buildLatestUri(UriInfo uriInfo) {
		return URI.create(uriInfo.getRequestUri() + "/latest");
	}

	URI buildLatestUri(String channelName, UriInfo uriInfo) {
		return URI.create(uriInfo.getRequestUri() + "/" + channelName + "/latest");
	}

	public URI buildItemUri(ContentKey key, URI channelUri) {
        return buildItemUri(key.keyToString(), channelUri);
	}

    public URI buildItemUri(String key, URI channelUri) {
        return URI.create(channelUri.toString() + "/" + key);
    }

	public URI buildWsLinkFor(UriInfo uriInfo) {
		String requestUri = uriInfo.getRequestUri().toString().replaceFirst("^http", "ws");
		return URI.create(requestUri + "/ws");
	}

	public URI buildWsLinkFor(String channelName, UriInfo uriInfo) {
		String requestUri = uriInfo.getRequestUri().toString().replaceFirst("^http", "ws");
		return URI.create(requestUri + "/" + channelName + "/ws");
	}

	public Linked<ChannelConfiguration> buildLinkedChannelConfig(ChannelConfiguration newConfig, URI channelUri, UriInfo uriInfo) {
		return linked(newConfig)
			.withLink("self", channelUri)
			.withLink("latest", buildLatestUri(newConfig.getName(), uriInfo))
			.withLink("ws", buildWsLinkFor(newConfig.getName(), uriInfo))
			.build();
	}
}
