package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.DecommissionManager;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.cluster.SpokeDecommissionManager;
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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("WeakerAccess")
@Path("/internal/cluster")
public class InternalClusterResource {
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final Cluster spokeCluster = HubProvider.getInstance(Cluster.class, "SpokeCluster");
    private static final DecommissionManager decommissionManager = HubProvider.getInstance(SpokeDecommissionManager.class);
    private static final SpokeDecommissionCluster decommissionCluster = HubProvider.getInstance(SpokeDecommissionCluster.class);

    public static final String DESCRIPTION = "Information about the cluster and decommissioning nodes.";
    private @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() throws Exception {
        URI requestUri = uriInfo.getRequestUri();
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        root.put("/decommission", "POSTing to /decommission will remove the localhost from Spoke writes.  " +
                "The server will continue to receive Spoke queries until all of its data is expired from Spoke.");
        root.put("/commission/{server}", "POSTing to /commission/{server} with the ip address of a previously decommissioned " +
                "server will allow that server to rejoin the cluster.  The new server should be started after this command.");
        addNodes("spokeCluster", spokeCluster.getAllServers(), root);
        addNodes("decommissioned.withinSpokeTTL", decommissionCluster.getWithinSpokeTTL(), root);
        addNodes("decommissioned.doNotStart", decommissionCluster.getDoNotRestart(), root);

        String localhostLink = HubHost.getLocalhostUri() + requestUri.getPath();

        Linked.Builder<?> links = Linked.linked(root);
        links.withLink("self", requestUri);
        links.withLink("decommission", localhostLink + "/decommission");
        links.withLink("commission", localhostLink + "/commission");
        return Response.ok(links.build()).build();
    }


    private void addNodes(String name, Collection<String> servers, ObjectNode root) throws UnknownHostException {
        ArrayNode cluster = root.putArray(name);
        Set<String> allServers = new TreeSet<>(servers);
        for (String server : allServers) {
            ObjectNode node = cluster.addObject();
            String ip = StringUtils.substringBefore(server, ":");
            node.put("ip", ip);
            //todo - gfm - this isn't working
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
