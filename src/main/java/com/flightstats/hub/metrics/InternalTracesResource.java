package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.Cluster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("WeakerAccess")
@Path("/internal/traces")
public class InternalTracesResource {

    public static final String DESCRIPTION = "Shows active requests, the slowest 100, and the latest 100 with links to other hubs in the cluster";
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final Cluster curatorCluster = HubProvider.getInstance(Cluster.class, "HubCluster");

    public static ObjectNode serverAndServers(String path) {
        ObjectNode root = mapper.createObjectNode();
        root.put("server", HubHost.getLocalHttpNameUri() + path);
        ArrayNode servers = root.putArray("servers");
        for (String spokeServer : curatorCluster.getAllServers()) {
            servers.add(HubHost.getScheme() + spokeServer + path);
        }
        return root;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = serverAndServers("/internal/traces");
        ActiveTraces.log(root);
        return Response.ok(root).build();
    }
}
