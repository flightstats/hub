package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/alert")
public class AlertResource {

    private final static Logger logger = LoggerFactory.getLogger(AlertResource.class);
    @Inject
    private ObjectMapper mapper;
    @Inject
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAlerts() {
        List<AlertConfig> alertConfigs = AlertConfigs.getLatest();
        Map<String, URI> nameUriMap = new HashMap<>();
        for (AlertConfig alertConfig : alertConfigs) {
            String name = alertConfig.getName();
            nameUriMap.put(name, URI.create(uriInfo.getBaseUri() + "alert/" + name));
        }
        //todo - gfm - 6/10/15 - add link to health
        Linked<?> result = LinkBuilder.buildLinks(uriInfo, nameUriMap, "alerts");
        return Response.ok(result).build();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealth() {
        Optional<ContentKey> latestKey = AlertStatuses.getLatestKey();
        logger.debug("latest key {}", latestKey);
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
