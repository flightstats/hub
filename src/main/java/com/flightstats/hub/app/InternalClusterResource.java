package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.DecommissionManager;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.rest.Linked;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.flightstats.hub.constant.InternalResourceDescription.CLUSTER_DESCRIPTION;

@Path("/internal/cluster")
public class InternalClusterResource {

    private final Cluster spokeCluster;
    private final DecommissionManager decommissionManager;
    private final SpokeDecommissionCluster decommissionCluster;
    private final ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    public InternalClusterResource(@Named("SpokeCluster") Cluster spokeCluster,
                                   DecommissionManager decommissionManager,
                                   SpokeDecommissionCluster decommissionCluster,
                                   ObjectMapper objectMapper) {
        this.spokeCluster = spokeCluster;
        this.decommissionManager = decommissionManager;
        this.decommissionCluster = decommissionCluster;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() throws Exception {
        URI requestUri = uriInfo.getRequestUri();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("description", CLUSTER_DESCRIPTION);
        root.put("directions", "Make HTTP POSTs to links below to take the desired action");
        root.put("/decommission", "POSTing to /decommission will remove the localhost from Spoke writes.  " +
                "The server will continue to receive Spoke queries until all of its data is expired from Spoke.");
        root.put("/recommission/{server}", "POSTing to /recommission/{server} with the ip address and port of a previously decommissioned " +
                "server will allow that server to rejoin the cluster.  The new server should be started after this command.");
        addNodes("spokeCluster", spokeCluster.getAllServers(), root);
        ObjectNode decommissioned = root.putObject("decommissioned");
        addNodes("withinSpokeTTL", decommissionCluster.getWithinSpokeTTL(), decommissioned);
        List<String> doNotRestart = decommissionCluster.getDoNotRestart();
        addNodes("doNotStart", doNotRestart, decommissioned);
        String localhostLink = HubHost.getLocalhostUri() + requestUri.getPath();
        Linked.Builder<?> links = Linked.linked(root);
        links.withLink("self", requestUri);
        links.withLink("decommission", localhostLink + "/decommission");
        for (String server : doNotRestart) {
            links.withLink("recommission " + server, localhostLink + "/recommission/" + server);
        }
        return Response.ok(links.build()).build();
    }

    private void addNodes(String name, Collection<String> servers, ObjectNode root) {
        ArrayNode cluster = root.putArray(name);
        Set<String> allServers = new TreeSet<>(servers);
        for (String server : allServers) {
            ObjectNode node = cluster.addObject();
            node.put("server", server);
        }
    }

    @POST
    @Path("decommission")
    public Response decommission() throws Exception {
        return LocalHostOnly.getResponse(uriInfo, decommissionManager::decommission);
    }

    @POST
    @Path("recommission/{server}")
    public Response recommission(@PathParam("server") String server) throws Exception {
        decommissionManager.recommission(server);
        return Response.accepted().build();
    }
}
