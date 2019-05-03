package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.util.HubUtils;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;
import java.util.SortedSet;

@Path("/channel/{channel}/status")
public class ChannelStatusResource {

    private final ContentRetriever contentRetriever;
    private final HubUtils hubUtils;
    private final ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    public ChannelStatusResource(ContentRetriever contentRetriever,
                                 HubUtils hubUtils,
                                 ObjectMapper objectMapper) {
        this.contentRetriever = contentRetriever;
        this.hubUtils = hubUtils;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatest(@PathParam("channel") String channel,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace) {
        final ChannelConfig channelConfig = contentRetriever.getCachedChannelConfig(channel)
                .orElseThrow(() -> {
                    throw new WebApplicationException(Response.status(404).build());
                });
        final ObjectNode root = objectMapper.createObjectNode();
        final ObjectNode links = root.putObject("_links");
        final ObjectNode self = links.putObject("self");
        final String baseUri = uriInfo.getRequestUri().toString();
        self.put("href", baseUri);

        addLink("latest", contentRetriever.getLatest(channel, stable), channel, links);

        final DirectionQuery directionQuery = ChannelEarliestResource.getDirectionQuery(channel, 1, stable,
                Location.ALL.name(), Epoch.IMMUTABLE.name());
        final SortedSet<ContentKey> earliest = contentRetriever.query(directionQuery);
        if (earliest.isEmpty()) {
            addLink("earliest", Optional.empty(), channel, links);
        } else {
            addLink("earliest", Optional.of(earliest.first()), channel, links);
        }

        if (contentRetriever.isReplicating(channel)) {
            final ObjectNode replicationSourceLatest = links.putObject("replicationSourceLatest");
            final Optional<String> sourceLatest = hubUtils.getLatest(channelConfig.getReplicationSource());
            if (sourceLatest.isPresent()) {
                replicationSourceLatest.put("href", sourceLatest.get());
            } else {
                replicationSourceLatest.put("href", channelConfig.getReplicationSource() + "/latest");
                replicationSourceLatest.put("message", "channel is empty");
            }
        }

        return Response.ok(root).build();
    }

    private void addLink(String name, Optional<ContentKey> contentKey, String channel, ObjectNode links) {
        final ObjectNode latestNode = links.putObject(name);
        if (contentKey.isPresent()) {
            latestNode.put("href", uriInfo.getBaseUri() + "channel/" + channel + "/" + contentKey.get().toUrl());
        } else {
            latestNode.put("href", uriInfo.getBaseUri() + "channel/" + channel + "/" + name);
            latestNode.put("message", "channel is empty");
        }
    }

}
