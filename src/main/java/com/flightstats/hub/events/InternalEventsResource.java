package com.flightstats.hub.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
@Path("/internal/events/{id}")
public class InternalEventsResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalEventsResource.class);

    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static EventsService eventsService = HubProvider.getInstance(EventsService.class);

    @POST
    public Response putPayload(@PathParam("id") String id, String data) {
        logger.trace("incoming {} {}", id, data);
        try {
            JsonNode node = mapper.readTree(data);
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
            logger.warn("unable to send to " + id + " data" + data, e);
            return Response.serverError().build();
        }
    }


}
