package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("WeakerAccess")
@Path("/health/metrics")
public class MetricsResource {
    private final static Logger logger = LoggerFactory.getLogger(MetricsResource.class);

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

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
