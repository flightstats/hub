package com.flightstats.hub.events;

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
@Path("/internal/events/{id}")
public class InternalEventsResource {

    private final ObjectMapper objectMapper;
    private final EventsService eventsService;

    @Inject
    public InternalEventsResource(ObjectMapper objectMapper, EventsService eventsService) {
        this.objectMapper = objectMapper;
        this.eventsService = eventsService;
    }

    @POST
    public Response putPayload(@PathParam("id") String id, String data) {
        log.trace("incoming {} {}", id, data);
        try {
            JsonNode node = objectMapper.readTree(data);
            if (node.get("type").asText().equals("heartbeat")) {
                eventsService.checkHealth(id);
            } else {
                ArrayNode uris = (ArrayNode) node.get("uris");
                for (JsonNode uri : uris) {
                    eventsService.getAndSendData(uri.asText(), id);
                }
            }
            return Response.ok().build();
        } catch (IOException e) {
            log.warn("unable to send to " + id + " data" + data, e);
            return Response.serverError().build();
        }
    }

}