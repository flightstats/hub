package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.rest.Headers;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.Sleeper;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.*;

@Path("/internal/replication/{channel}")
public class ReplicationCallbackResource {

    private final static Logger logger = LoggerFactory.getLogger(ReplicationCallbackResource.class);

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private LastContentPath lastContentPath = HubProvider.getInstance(LastContentPath.class);

    @POST
    public Response putPayload(@PathParam("channel") String channel, String data) {
        logger.trace("incoming {} {}", channel, data);
        try {
            JsonNode node = mapper.readTree(data);
            ArrayNode uris = (ArrayNode) node.get("uris");
            for (JsonNode uri : uris) {
                String contentUrl = uri.asText();
                ClientResponse response = hubUtils.getResponse(contentUrl);
                if (CLIENT_ERROR.equals(response.getStatusInfo().getFamily())) {
                    logger.info("first client error {}", response);
                    Sleeper.sleep(5000);
                    response = hubUtils.getResponse(contentUrl);
                    if (!SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                        logger.warn("second client error {}", response);
                        return Response.ok().build();
                    }
                } else if (SERVER_ERROR.equals(response.getStatusInfo().getFamily())) {
                    logger.warn("server error {}", response);
                    return Response.status(500).build();
                }
                Content content = Content.builder()
                        .withContentKey(ContentKey.fromFullUrl(contentUrl).get())
                        .withContentType(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                        .withContentLanguage(response.getHeaders().getFirst(Headers.LANGUAGE))
                        .withData(response.getEntity(byte[].class))
                        .build();
                channelService.insert(channel, content);
            }
            if (node.has("id")) {
                String id = node.get("id").asText();
                logger.trace("repl id {} for {}", id, channel);
                Optional<MinutePath> pathOptional = MinutePath.fromUrl(id);
                if (pathOptional.isPresent()) {
                    lastContentPath.updateIncrease(pathOptional.get(), channel, ChannelReplicator.REPLICATED_LAST_UPDATED);
                }
            }
        } catch (Exception e) {
            logger.warn("unable to parse " + data, e);
        }
        return Response.ok().build();
    }

}
