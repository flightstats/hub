package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.Linked;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static com.flightstats.hub.rest.Linked.linked;

//todo - gfm - 1/22/15 - convert this to all static
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

    public static URI buildChannelUri(ChannelConfiguration channelConfiguration, UriInfo uriInfo) {
        return buildChannelUri(channelConfiguration.getName(), uriInfo);
    }

    static URI buildChannelUri(String channelName, UriInfo uriInfo) {
        return URI.create(uriInfo.getBaseUri() + "channel/" + channelName);
    }

    URI buildTagUri(String tag, UriInfo uriInfo) {
        return URI.create(uriInfo.getBaseUri() + "tag/" + tag);
    }

    public static URI buildItemUri(ContentKey key, URI channelUri) {
        return buildItemUri(key.toUrl(), channelUri);
    }

    public static URI buildItemUri(String key, URI channelUri) {
        return URI.create(channelUri.toString() + "/" + key);
    }

    public Linked<ChannelConfiguration> buildChannelLinks(ChannelConfiguration config, URI channelUri) {
        Linked.Builder<ChannelConfiguration> linked = linked(config).withLink("self", channelUri);
        linked.withLink("latest", URI.create(channelUri + "/latest"))
                .withLink("ws", buildWsLinkFor(channelUri))
                .withLink("time", URI.create(channelUri + "/time"))
                .withLink("status", URI.create(channelUri + "/status"));
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

    public static Response directionalResponse(String channel, Collection<ContentKey> keys, int count,
                                               DirectionQuery query, ObjectMapper mapper, UriInfo uriInfo) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        List<ContentKey> list = new ArrayList<>(keys);
        if (!list.isEmpty()) {
            String baseUri = uriInfo.getBaseUri() + "channel/" + channel + "/";
            ObjectNode next = links.putObject("next");
            next.put("href", baseUri + list.get(list.size() - 1).toUrl() + "/next/" + count);
            ObjectNode previous = links.putObject("previous");
            previous.put("href", baseUri + list.get(0).toUrl() + "/previous/" + count);
        }
        ArrayNode ids = links.putArray("uris");
        URI channelUri = buildChannelUri(channel, uriInfo);
        for (ContentKey key : keys) {
            URI uri = buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        query.getTraces().output(root);
        return Response.ok(root).build();
    }
}
