package com.flightstats.hub.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/internal/ws/{id}")
public class WebSocketResource {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketResource.class);

    private final WebSocketService webSocketService;
    private final ObjectMapper mapper;

    @Inject
    WebSocketResource(WebSocketService webSocketService, ObjectMapper mapper) {
        this.webSocketService = webSocketService;
        this.mapper = mapper;
    }

    @POST
    public Response putPayload(@PathParam("id") String id, String data) {
        logger.trace("incoming {} {}", id, data);
        try {
            JsonNode node = mapper.readTree(data);
            ArrayNode uris = (ArrayNode) node.get("uris");
            for (JsonNode uri : uris) {
                webSocketService.call(id, uri.asText());
            }
        } catch (IOException e) {
            logger.warn("unable to parse " + data, e);
        }
        return Response.ok().build();
    }

}
