package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.cluster.DynamicSpokeCluster;
import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@SuppressWarnings("WeakerAccess")
@Path("/internal/cluster")
public class InternalClusterResource {
    public static final String DESCRIPTION = "View and modify the cluster's status.";
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final DynamicSpokeCluster dynamicSpokeCluster = HubProvider.getInstance(DynamicSpokeCluster.class);


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkCluster(@Context UriInfo uriInfo) {
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        URI requestUri = uriInfo.getRequestUri();
        String localhostLink = HubHost.getLocalhostUri() + requestUri.getPath();
        dynamicSpokeCluster.status(root);
        Linked.Builder<?> links = Linked.linked(root);
        links.withLink("self", requestUri);
        links.withLink("decommission", localhostLink + "/decommission");
        return Response.ok(links.build()).build();
    }

    @POST
    @Path("decommission")
    public Response decommission(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> {
            dynamicSpokeCluster.decommission();
            return Response.ok();
        });
    }
}
