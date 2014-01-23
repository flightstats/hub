package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.migration.ChannelUtils;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This is a convenience interface for external data Providers.
 * It supports automatic channel creation and does not return links they can not access.
 */
@Path("/migration/{host}/{channel}")
public class MigratorResource {
    private final static Logger logger = LoggerFactory.getLogger(MigratorResource.class);
    private final ChannelService channelService;
    private final ChannelUtils channelUtils;

    @Inject
    public MigratorResource(ChannelService channelService, ChannelUtils channelUtils ) {
        this.channelService = channelService;
        this.channelUtils = channelUtils;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@PathParam("host") final String host,
                                @PathParam("channel") final String channel) throws Exception {

        /*ChannelMigrator migrator = new ChannelMigrator(channelService, host, channel, channelUtils);
        new Thread(migrator, host + "_" + channel).start();*/
        return Response.status(Response.Status.ACCEPTED).build();
    }


}
