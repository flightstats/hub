package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.cluster.Cluster;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/internal/traces")
public class InternalTracesResource {

    private final Cluster curatorCluster;
    private final ObjectMapper objectMapper;

    @Inject
    public InternalTracesResource(@Named("HubCluster") Cluster curatorCluster,
                                  ObjectMapper objectMapper) {
        this.curatorCluster = curatorCluster;
        this.objectMapper = objectMapper;
    }

    public ObjectNode serverAndServers(String path) {
        final ObjectNode root = this.objectMapper.createObjectNode();
        root.put("server", HubHost.getLocalHttpNameUri() + path);
        ArrayNode servers = root.putArray("servers");
        for (String spokeServer : this.curatorCluster.getAllServers()) {
            servers.add(HubHost.getScheme() + spokeServer + path);
        }
        return root;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        final ObjectNode root = serverAndServers("/internal/traces");
        ActiveTraces.log(root);
        return Response.ok(root).build();
    }
}
