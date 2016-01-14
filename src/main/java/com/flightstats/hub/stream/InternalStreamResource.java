package com.flightstats.hub.stream;

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

@Path("/internal/stream/{id}")
public class InternalStreamResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalStreamResource.class);


    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private StreamService streamService = HubProvider.getInstance(StreamService.class);

    @POST
    public Response putPayload(@PathParam("id") String id, String data) {
        logger.trace("incoming {} {}", id, data);
        try {
            JsonNode node = mapper.readTree(data);
            ArrayNode uris = (ArrayNode) node.get("uris");
            for (JsonNode uri : uris) {
                streamService.getAndSendData(uri.asText(), id);
            }
        } catch (IOException e) {
            logger.warn("unable to send to " + id + " data" + data, e);
        }
        return Response.ok().build();
    }


}
