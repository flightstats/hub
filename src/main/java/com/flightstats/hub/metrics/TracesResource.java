package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.cluster.CuratorCluster;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/internal/traces")
public class TracesResource {
    private final static Logger logger = LoggerFactory.getLogger(TracesResource.class);
    @Inject
    private ObjectMapper mapper;
    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster hubCuratorCluster;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = mapper.createObjectNode();

        String tracesPath = "/internal/traces";
        root.put("server", HubHost.getLocalHttpNameUri() + tracesPath);
        ArrayNode servers = root.putArray("servers");
        for (String spokeServer : hubCuratorCluster.getServers()) {
            servers.add(HubHost.getScheme() + spokeServer + tracesPath);
        }
        ActiveTraces.log(root);
        return Response.ok(root).build();
    }
}
