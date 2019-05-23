package com.flightstats.hub.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Slf4j
@Path("/internal/ws/{id}")
public class WebSocketResource {

    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Inject
    public WebSocketResource(WebSocketService webSocketService, ObjectMapper objectMapper) {
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
    }

    @POST
    public Response putPayload(@PathParam("id") String id, String data) {
        log.trace("incoming {} {}", id, data);
        try {
            JsonNode node = objectMapper.readTree(data);
            ArrayNode uris = (ArrayNode) node.get("uris");
            for (JsonNode uri : uris) {
                this.webSocketService.call(id, uri.asText());
            }
        } catch (IOException e) {
            log.warn("unable to parse " + data, e);
        }
        return Response.ok().build();
    }

}
