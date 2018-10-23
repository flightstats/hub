package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.rest.Linked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

@Path("/channel")
public class ChannelsResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelsResource.class);

    private final ChannelService channelService;

    @Context
    private UriInfo uriInfo;

    @Inject
    ChannelsResource(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        Map<String, URI> mappedUris = new TreeMap<>();
        for (ChannelConfig channelConfig : channelService.getChannels()) {
            String channelName = channelConfig.getDisplayName();
            mappedUris.put(channelName, LinkBuilder.buildChannelUri(channelName, uriInfo));
        }
        Linked<?> result = LinkBuilder.buildLinks(uriInfo, mappedUris, "channels");
        return Response.ok(result).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(String json) throws InvalidRequestException, ConflictException {
        logger.debug("post channel {}", json);
        ChannelConfig channelConfig = channelService.createFromJson(json);
        channelConfig = channelService.createChannel(channelConfig);
        URI channelUri = LinkBuilder.buildChannelUri(channelConfig.getDisplayName(), uriInfo);
        ObjectNode output = LinkBuilder.buildChannelConfigResponse(channelConfig, uriInfo, channelConfig.getDisplayName());
        return Response.created(channelUri).entity(output).build();
    }
}
