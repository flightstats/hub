package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.CuratorCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("WeakerAccess")
@Path("/internal/traces")
public class InternalTracesResource {
    private final static Logger logger = LoggerFactory.getLogger(InternalTracesResource.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final CuratorCluster hubCuratorCluster = HubProvider.getInstance(CuratorCluster.class, "HubCuratorCluster");

    static ObjectNode serverAndServers(String path) {
        ObjectNode root = mapper.createObjectNode();
        root.put("server", HubHost.getLocalHttpNameUri() + path);
        ArrayNode servers = root.putArray("servers");
        for (String spokeServer : hubCuratorCluster.getServers()) {
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
