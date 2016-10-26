package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.io.IOException;
import java.net.URI;

import static com.flightstats.hub.channel.LinkBuilder.buildChannelConfigResponse;

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
        if (oldConfig != null) {
            if (oldConfig.isGlobalSatellite()) {
                logger.warn("attempt to change master cluster location {} {}", oldConfig);
                return Response.status(400).entity("The Master cluster location is not allowed to change.").build();
            }
        }
        return create(channelName, json, true);
    }

    @PUT
    @Path("/satellite/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSatellite(@PathParam("channel") String channelName, String json) throws Exception {
        logger.info("put satellite {} {}", channelName, json);
        return create(channelName, json, false);
    }

    private Response create(@PathParam("channel") String channelName, String json, boolean isMaster) throws IOException {
        ChannelConfig newConfig = ChannelConfig.createFromJsonWithName(json, channelName);
        ChannelConfig oldConfig = channelService.getChannelConfig(channelName, false);
        if (oldConfig != null) {
            logger.info("using existing channel {} {}", oldConfig, newConfig);
            newConfig = ChannelConfig.updateFromJson(oldConfig, StringUtils.defaultIfBlank(json, "{}"));
        }
        newConfig.getGlobal().setIsMaster(isMaster);
        newConfig = channelService.updateChannel(newConfig, oldConfig, true);
        URI channelUri = LinkBuilder.buildChannelUri(newConfig.getName(), uriInfo);
        ObjectNode output = buildChannelConfigResponse(newConfig, uriInfo);
        return Response.created(channelUri).entity(output).build();
    }
}
