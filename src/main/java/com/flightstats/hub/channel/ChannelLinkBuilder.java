package com.flightstats.hub.channel;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.Linked;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flightstats.hub.rest.Linked.linked;

public class ChannelLinkBuilder {

    @Inject
    public ChannelLinkBuilder() {
    }

    public static URI buildWsLinkFor(URI channelUri) {
        String requestUri = channelUri.toString().replaceFirst("^http", "ws");
        return URI.create(requestUri + "/ws");
    }

    public static void addOptionalHeader(String headerName, Optional<String> headerValue, Response.ResponseBuilder builder) {
        if (headerValue.isPresent()) {
            builder.header(headerName, headerValue.get());
        }
    }

    URI buildChannelUri(ChannelConfiguration channelConfiguration, UriInfo uriInfo) {
        return buildChannelUri(channelConfiguration.getName(), uriInfo);
    }

    URI buildChannelUri(String channelName, UriInfo uriInfo) {
        return URI.create(uriInfo.getBaseUri() + "channel/" + channelName);
    }

    URI buildTagUri(String tag, UriInfo uriInfo) {
        return URI.create(uriInfo.getBaseUri() + "tag/" + tag);
    }

    public URI buildItemUri(ContentKey key, URI channelUri) {
        return buildItemUri(key.toUrl(), channelUri);
    }

    public URI buildItemUri(String key, URI channelUri) {
        return URI.create(channelUri.toString() + "/" + key);
    }

    public Linked<ChannelConfiguration> buildChannelLinks(ChannelConfiguration config, URI channelUri) {
        Linked.Builder<ChannelConfiguration> linked = linked(config).withLink("self", channelUri);
        linked.withLink("latest", URI.create(channelUri + "/latest"))
                .withLink("ws", buildWsLinkFor(channelUri))
                .withLink("time", URI.create(channelUri + "/time"));
        return linked.build();
    }

    public Linked<?> build(Iterable<ChannelConfiguration> channels, UriInfo uriInfo) {
        Map<String, URI> mappedChannels = new HashMap<>();
        for (ChannelConfiguration channelConfiguration : channels) {
            String channelName = channelConfiguration.getName();
            mappedChannels.put(channelName, buildChannelUri(channelName, uriInfo));
        }

        Linked.Builder responseBuilder = Linked.justLinks();
        responseBuilder.withLink("self", uriInfo.getRequestUri());

        List<HalLink> channelLinks = new ArrayList<>(mappedChannels.size());
        for (Map.Entry<String, URI> entry : mappedChannels.entrySet()) {
            HalLink link = new HalLink(entry.getKey(), entry.getValue());
            channelLinks.add(link);
        }
        responseBuilder.withLinks("channels", channelLinks);
        return responseBuilder.build();
    }
}
