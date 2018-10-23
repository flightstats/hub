package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/health/metrics")
public class MetricsResource {

    private final ObjectMapper mapper;

    @Inject
    MetricsResource(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("openFiles", MetricsRunner.getOpenFiles());
        return Response.ok(rootNode).build();
    }

    @GET
    @Path("trace")
    @Produces(MediaType.TEXT_PLAIN)
    public Response trigger() {
        return Response.ok(MetricsRunner.logFilesInfo()).build();
    }
}
