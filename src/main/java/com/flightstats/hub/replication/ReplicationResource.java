package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ChannelConfiguration;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/replication")
public class ReplicationResource {

    @Inject
    private ReplicationService replicationService;

    @Inject
    private UriInfo uriInfo;

    private static final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReplicatedChannels() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        String baseUri = uriInfo.getRequestUri().toString();
        self.put("href", baseUri);
        Iterable<ChannelConfiguration> channels = replicationService.getReplicatingChannels();
        ArrayNode replicated = links.putArray("replicated");
        for (ChannelConfiguration channel : channels) {
            ObjectNode object = replicated.addObject();
            object.put("name", channel.getName());
            object.put("href", baseUri + "/" + channel.getName());
            object.put("replicationSource", channel.getReplicationSource());
        }
        return Response.ok(root).build();
    }

    @GET
    @Path("/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReplicatedChannel(@PathParam("channelName") String channelName) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        String baseUri = uriInfo.getRequestUri().toString();
        self.put("href", baseUri);
        replicationService.getStatus(channelName, root, uriInfo);

        return Response.ok(root).build();
    }

}
