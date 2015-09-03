package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/internal/replication/{channel}")
public class ReplicationCallbackResource {

    private final static Logger logger = LoggerFactory.getLogger(ReplicationCallbackResource.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private ChannelService channelService;

    @Inject
    private HubUtils hubUtils;

    @POST
    public Response putPayload(@PathParam("channel") String channel, String data) {
        logger.trace("incoming {} {}", channel, data);
        try {
            JsonNode node = mapper.readTree(data);
            ArrayNode uris = (ArrayNode) node.get("uris");
            for (JsonNode uri : uris) {
                Optional<Content> content = hubUtils.getContent(uri.asText());
                if (content.isPresent()) {
                    channelService.insert(channel, content.get());
                } else {
                    logger.warn("unable to get channel {} content {}", channel, uri.asText());
                }
            }
        } catch (Exception e) {
            logger.warn("unable to parse " + data, e);
        }
        return Response.ok().build();
    }
}
