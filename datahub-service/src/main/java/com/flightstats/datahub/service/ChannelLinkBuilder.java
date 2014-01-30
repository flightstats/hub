package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

public class ChannelLinkBuilder {

	@Inject
	public ChannelLinkBuilder() {
	}

	URI buildChannelUri(ChannelConfiguration channelConfiguration, UriInfo uriInfo) {
		return buildChannelUri(channelConfiguration.getName(), uriInfo);
	}

	URI buildChannelUri(String channelName, UriInfo uriInfo) {
		return URI.create(uriInfo.getBaseUri() + "channel/" + channelName);
	}

    public URI buildItemUri(ContentKey key, URI channelUri) {
        return buildItemUri(key.keyToString(), channelUri);
	}

    public URI buildItemUri(String key, URI channelUri) {
        return URI.create(channelUri.toString() + "/" + key);
    }

	public static URI buildWsLinkFor(URI channelUri) {
		String requestUri = channelUri.toString().replaceFirst("^http", "ws");
		return URI.create(requestUri + "/ws");
	}

	public Linked<ChannelConfiguration> buildChannelLinks(ChannelConfiguration config, URI channelUri) {
        Linked.Builder<ChannelConfiguration> linked = linked(config).withLink("self", channelUri);
        if (config.isSequence()) {
            linked.withLink("latest", URI.create(channelUri + "/latest"))
                    .withLink("ws", buildWsLinkFor(channelUri));
        }

        linked.withLink("time", URI.create(channelUri + "/time"));
        return linked.build();
	}
}
