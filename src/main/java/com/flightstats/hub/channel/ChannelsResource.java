package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.rest.Linked;
import lombok.extern.slf4j.Slf4j;

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

import static com.flightstats.hub.channel.LinkBuilder.buildChannelConfigResponse;

/**
 * This resource represents the collection of all channels in the Hub.
 */
@Slf4j
@Path("/channel")
public class ChannelsResource {

    private final ChannelService channelService;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ChannelsResource(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        final Map<String, URI> mappedUris = new TreeMap<>();
        for (ChannelConfig channelConfig : channelService.getChannels()) {
            final String channelName = channelConfig.getDisplayName();
            mappedUris.put(channelName, LinkBuilder.buildChannelUri(channelName, uriInfo));
        }
        final Linked<?> result = LinkBuilder.buildLinks(uriInfo, mappedUris, "channels");
        return Response.ok(result).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(String json) throws InvalidRequestException, ConflictException {
        log.debug("post channel {}", json);
        ChannelConfig channelConfig = ChannelConfig.createFromJson(json);
        channelConfig = channelService.createChannel(channelConfig);
        final URI channelUri = LinkBuilder.buildChannelUri(channelConfig.getDisplayName(), uriInfo);
        final ObjectNode output = buildChannelConfigResponse(channelConfig, uriInfo, channelConfig.getDisplayName());
        return Response.created(channelUri).entity(output).build();
    }
}
