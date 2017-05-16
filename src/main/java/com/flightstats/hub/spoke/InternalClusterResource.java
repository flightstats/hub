package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.DynamicSpokeCluster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@SuppressWarnings("WeakerAccess")
@Path("/internal/cluster")
public class InternalClusterResource {
    public static final String DESCRIPTION = "See the cluster's status over time.";
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final DynamicSpokeCluster dynamicSpokeCluster = HubProvider.getInstance(DynamicSpokeCluster.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkCluster(@Context UriInfo uriInfo) {
        ObjectNode root = mapper.createObjectNode();
        dynamicSpokeCluster.status(root);
        return Response.ok(root).build();
    }
}
