package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.replication.ChannelUtils;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/channel/{channelName: .*}/status")
public class ChannelStatusResource {

    @Inject
    private UriInfo uriInfo;
    @Inject
    private ChannelService channelService;
    @Inject
    private ChannelUtils channelUtils;

    private static final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @EventTimed(name = "channel.ALL.status.get")
    public Response getLatest(@PathParam("channelName") String channelName,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        String baseUri = uriInfo.getRequestUri().toString();
        self.put("href", baseUri);

        Optional<ContentKey> latest = channelService.getLatest(channelName, stable, trace);
        ObjectNode latestNode = links.putObject("latest");
        if (latest.isPresent()) {
            latestNode.put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + latest.get().toUrl());
        } else {
            latestNode.put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/latest");
            latestNode.put("message", "channel is empty");
        }
        if (channelService.isReplicating(channelName)) {
            ChannelConfiguration config = channelService.getChannelConfiguration(channelName);
            ObjectNode replicationSourceLatest = links.putObject("replicationSourceLatest");
            Optional<String> sourceLatest = channelUtils.getLatest(config.getReplicationSource());
            if (sourceLatest.isPresent()) {
                replicationSourceLatest.put("href", sourceLatest.get());
            } else {
                replicationSourceLatest.put("href", config.getReplicationSource() + "/latest");
                replicationSourceLatest.put("message", "channel is empty");
            }
        }

        return Response.ok(root).build();
    }

}
