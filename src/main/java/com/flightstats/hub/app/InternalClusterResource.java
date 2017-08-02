package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.DecommissionManager;
import com.flightstats.hub.rest.Linked;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
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
    private static final DecommissionManager decommissionManager = HubProvider.getInstance(DecommissionManager.class);

    public static final String DESCRIPTION = "Information about the cluster";
    private @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() throws Exception {
        URI requestUri = uriInfo.getRequestUri();
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        //root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        /*


        //todo - gfm - add decommission method
        decomm removes the node from the write pool.
        the node remains in the query pool
        if the node is restarted, it should not start, with an error message on how to fix it
        //todo - gfm - also provide a mechanism to clear/modify the decomm list
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
            String ip = StringUtils.substringBefore(server, ":");
            node.put("ip", ip);
            //todo - gfm - resolve the hostname with a direct call?
            //todo - gfm - or should we just put this in /internal/health ?
            node.put("name", InetAddress.getByName(ip).getHostName());
        }
    }

    @POST
    @Path("decommission")
    public Response decommission() throws Exception {
        return LocalHostOnly.getResponse(uriInfo, decommissionManager::decommission);
    }


    @POST
    @Path("commission/{server}")
    public Response commission(@PathParam("server") String server) throws Exception {
        decommissionManager.commission(server);
        return Response.accepted().build();
    }
}
