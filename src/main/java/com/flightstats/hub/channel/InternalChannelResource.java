package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.HubUtils;
import com.google.inject.TypeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/internal/channel")
public class InternalChannelResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalChannelResource.class);

    @Context
    private UriInfo uriInfo;
    private final static Dao<ChannelConfig> channelConfigDao = HubProvider.getInstance(
            new TypeLiteral<Dao<ChannelConfig>>() {
            }, "ChannelConfig");
    private final static HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    public static final String DESCRIPTION = "GET to refresh of the Channel Cache within the entire hub cluster.";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) throws Exception {
        Linked.Builder<?> links = Linked.linked(DESCRIPTION);
        links.withLink("self", uriInfo.getRequestUri());
        links.withRelativeLink("refresh", uriInfo);
        return Response.ok(links.build()).build();
    }

    @GET
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@QueryParam("all") @DefaultValue("true") boolean all) throws Exception {
        logger.info("refreshing all = {}", all);
        if (all) {
            return Response.ok(hubUtils.refreshAll()).build();
        } else {
            if (channelConfigDao.refresh()) {
                return Response.ok(HubHost.getLocalNamePort()).build();
            } else {
                return Response.status(400).entity(HubHost.getLocalNamePort()).build();
            }
        }
    }

    @Path("{channel}")
    @DELETE
    public Response delete(@PathParam("channel") final String channelName) throws Exception {
        ChannelConfig channelConfig = channelService.getChannelConfig(channelName, false);
        if (channelConfig == null) {
            return ChannelResource.notFound(channelName);
        }
        if (HubProperties.isProtected()) {
            logger.info("using internal localhost only to delete {}", channelName);
            return LocalHostOnly.getResponse(uriInfo, () -> ChannelResource.deletion(channelName));
        }
        logger.info("using internal delete {}", channelName);
        return ChannelResource.deletion(channelName);
    }

}
