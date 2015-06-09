package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/alert")
public class AlertResource {

    private final static Logger logger = LoggerFactory.getLogger(AlertResource.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelMetadata() {
        Optional<ContentKey> latestKey = AlertStatuses.getLatestKey();
        logger.info("latest key {}", latestKey);
        ObjectNode objectNode = mapper.createObjectNode();
        if (latestKey.isPresent()) {
            ContentKey key = latestKey.get();
            objectNode.put("latestStatusKey", key.toString());
            int minutes = Minutes.minutesBetween(key.getTime(), TimeUtil.now()).getMinutes();
            objectNode.put("statusOffsetMinutes", minutes);
            if (minutes <= 2) {
                return Response.ok(objectNode.toString()).build();
            }
        } else {
            objectNode.put("latestStatusKey", "none");
        }
        return Response.status(500).entity(objectNode.toString()).build();
    }

}
