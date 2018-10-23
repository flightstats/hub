package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.cluster.CuratorCluster;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/internal/traces")
public class InternalTracesResource {

    public final static String DESCRIPTION = "Shows active requests, the slowest 100, and the latest 100 with links to other hubs in the cluster";

    private final CuratorCluster curatorCluster;
    private final ObjectMapper mapper;

    @Inject
    InternalTracesResource(@Named("HubCluster") CuratorCluster curatorCluster, ObjectMapper mapper) {
        this.curatorCluster = curatorCluster;
        this.mapper = mapper;
    }

    public ObjectNode serverAndServers(String path) {
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
