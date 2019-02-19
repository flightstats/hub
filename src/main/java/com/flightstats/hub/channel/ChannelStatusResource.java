package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.HubUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;
import java.util.SortedSet;

@SuppressWarnings("WeakerAccess")
@Path("/channel/{channel}/status")
public class ChannelStatusResource {

    @Context
    private UriInfo uriInfo;

    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private final static HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatest(@PathParam("channel") String channel,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace) {
        ChannelConfig channelConfig = channelService.getCachedChannelConfig(channel);
        if (null == channelConfig) {
            return Response.status(404).build();
        }
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        String baseUri = uriInfo.getRequestUri().toString();
        self.put("href", baseUri);

        addLink("latest", channelService.getLatest(channel, stable), channel, links);

        DirectionQuery directionQuery = ChannelEarliestResource.getDirectionQuery(channel, 1, stable,
                Location.ALL.name(), Epoch.IMMUTABLE.name());
        SortedSet<ContentKey> earliest = channelService.query(directionQuery);
        if (earliest.isEmpty()) {
            addLink("earliest", Optional.empty(), channel, links);
        } else {
            addLink("earliest", Optional.of(earliest.first()), channel, links);
        }

        if (channelService.isReplicating(channel)) {
            ObjectNode replicationSourceLatest = links.putObject("replicationSourceLatest");
            Optional<String> sourceLatest = hubUtils.getLatest(channelConfig.getReplicationSource());
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
        ObjectNode latestNode = links.putObject(name);
        if (contentKey.isPresent()) {
            latestNode.put("href", uriInfo.getBaseUri() + "channel/" + channel + "/" + contentKey.get().toUrl());
        } else {
            latestNode.put("href", uriInfo.getBaseUri() + "channel/" + channel + "/" + name);
            latestNode.put("message", "channel is empty");
        }
    }

}
