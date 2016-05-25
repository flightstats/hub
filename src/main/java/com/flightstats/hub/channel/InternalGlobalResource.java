package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.LocalChannelService;
import com.flightstats.hub.model.ChannelConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("/internal/global")
public class InternalGlobalResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalGlobalResource.class);
    private final static ChannelService channelService = HubProvider.getInstance(LocalChannelService.class);
    @Context
    private UriInfo uriInfo;

    @PUT
    @Path("/master/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMaster(@PathParam("channel") String channelName, String json) throws Exception {
        logger.info("put master {} {}", channelName, json);
        ChannelConfig oldConfig = channelService.getChannelConfig(channelName, false);
        ChannelConfig newConfig = ChannelConfig.fromJsonName(json, channelName);
        if (oldConfig != null) {
            if (oldConfig.isGlobalSatellite()) {
                logger.warn("attempt to change master cluster location {} {}", oldConfig, newConfig);
                return Response.status(400).entity("The Master cluster location is not allowed to change.").build();
            }
            logger.info("using existing channel {} {}", oldConfig, newConfig);
            newConfig = ChannelConfig.builder()
                    .withChannelConfiguration(oldConfig)
                    .withUpdateJson(StringUtils.defaultIfBlank(json, "{}"))
                    .build();
        }
        newConfig.getGlobal().setIsMaster(true);
        newConfig = channelService.updateChannel(newConfig, oldConfig);
        URI channelUri = LinkBuilder.buildChannelUri(newConfig, uriInfo);
        return Response.created(channelUri).entity(
                LinkBuilder.buildChannelLinks(newConfig, channelUri))
                .build();

        /*
        //todo - gfm - 5/20/16 -
        call /internal/global/satellite/{channel}
        this can be async, does not need to succeed for the creation request to complete
        Also needs to create/update replication for the channels

        //todo - gfm - 5/20/16 - can this use the normal replication endpoint?
        for creation, make sure we start replication at the creation time of the channel
        */

    }

    @PUT
    @Path("/satellite/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSatellite(@PathParam("channel") String channelName, String json) throws Exception {
        //todo - gfm - 5/20/16 - make sure master is not set...
        //return channelResource.createChannel(channelName, json);
        return null;
    }
}
