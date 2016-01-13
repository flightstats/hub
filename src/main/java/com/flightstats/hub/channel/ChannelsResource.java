package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.rest.Linked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * This resource represents the collection of all channels in the Hub.
 */
@Path("/channel")
public class ChannelsResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelsResource.class);

    @Context
    private UriInfo uriInfo;

    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    @EventTimed(name = "channels.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        Iterable<ChannelConfig> channels = channelService.getChannels();
        Linked<?> result = LinkBuilder.build(channels, uriInfo);
        return Response.ok(result).build();
    }

    @POST
    @EventTimed(name = "channels.post")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(String json) throws InvalidRequestException, ConflictException {
        logger.debug("post channel {}", json);
        ChannelConfig channelConfig = ChannelConfig.fromJson(json);
        channelConfig = channelService.createChannel(channelConfig);
        URI channelUri = LinkBuilder.buildChannelUri(channelConfig, uriInfo);
        return Response.created(channelUri).entity(
                LinkBuilder.buildChannelLinks(channelConfig, channelUri))
                .build();
    }
}
