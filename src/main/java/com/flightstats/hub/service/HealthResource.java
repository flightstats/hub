package com.flightstats.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.model.HealthStatus;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/health")
public class HealthResource {
    private final static Logger logger = LoggerFactory.getLogger(HealthResource.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static String version;

    @Inject
    HubHealthCheck healthCheck;

    @Inject
    HubVersion hubVersion;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
        HealthStatus healthStatus = healthCheck.getStatus();
        rootNode.put("healthy", healthStatus.isHealthy());
        rootNode.put("description", healthStatus.getDescription());
        rootNode.put("version", hubVersion.getVersion());
        rootNode.put("startTime", HubMain.getStartTime().toString());

        if (healthStatus.isHealthy()) {
            return Response.ok(rootNode).build();
        } else {
            return Response.serverError().entity(rootNode).build();
        }
    }


}
