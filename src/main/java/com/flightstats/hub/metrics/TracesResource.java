package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.spoke.CuratorSpokeCluster;
import com.google.inject.Inject;
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
    private CuratorSpokeCluster curatorSpokeCluster;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = mapper.createObjectNode();

        String tracesPath = "/internal/traces";
        root.put("server", HubHost.getLocalUriRoot() + tracesPath);
        ArrayNode servers = root.putArray("servers");
        //todo - gfm - 11/18/15 - this should also include the batch server, if available
        //todo - gfm - 11/18/15 - this also does not work for NAS cluster
        for (String spokeServer : curatorSpokeCluster.getServers()) {
            servers.add(HubHost.getScheme() + spokeServer + tracesPath);
        }
        ActiveTraces.log(root);
        return Response.ok(root).build();
    }
}
