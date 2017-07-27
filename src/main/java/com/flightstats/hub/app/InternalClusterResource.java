package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("WeakerAccess")
@Path("/internal/cluster")
public class InternalClusterResource {
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final Cluster spokeCluster = HubProvider.getInstance(Cluster.class, "SpokeCluster");

    public static final String DESCRIPTION = "Information about the cluster";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) throws Exception {
        URI requestUri = uriInfo.getRequestUri();
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        //root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        /*

        todo - provide mechanism to change the write factor?
        todo - could we change the write factor by channel?
         */
        addCluster(root);

        Linked.Builder<?> links = Linked.linked(root);
        links.withLink("self", requestUri);
        return Response.ok(links.build()).build();
    }

    private void addCluster(ObjectNode root) throws UnknownHostException {
        ArrayNode cluster = root.putArray("cluster");
        Set<String> allServers = new TreeSet<>(spokeCluster.getAllServers());
        for (String server : allServers) {
            ObjectNode node = cluster.addObject();
            node.put("ip", server);
            node.put("name", InetAddress.getByName(server).getHostName());
        }
    }

    @POST
    public Response shutdown(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> getManager().shutdown(true));
    }

    @POST
    @Path("resetLock")
    public Response resetLock(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> getManager().resetLock());
    }

    private static ShutdownManager getManager() {
        return HubProvider.getInstance(ShutdownManager.class);
    }

}
