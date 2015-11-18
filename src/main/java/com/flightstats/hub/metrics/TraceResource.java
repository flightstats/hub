package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/internal/trace")
public class TraceResource {
    private final static Logger logger = LoggerFactory.getLogger(TraceResource.class);
    @Inject
    private ObjectMapper mapper;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = mapper.createObjectNode();
        ActiveTraces.log(root);
        return Response.ok(root).build();
    }
}
