package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/internal/global")
public class GlobalChannelResource {

    private final static Logger logger = LoggerFactory.getLogger(GlobalChannelResource.class);

    private final static ChannelResource channelResource = HubProvider.getInstance(ChannelResource.class);

    @PUT
    @Path("/master/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMaster(@PathParam("channel") String channelName, String json) throws Exception {


        //todo - gfm - 5/20/16 - probably want something other than the Response object
        Response channel = channelResource.createChannel(channelName, json);
        /*
        //todo - gfm - 5/20/16 - do not allow the master url to change - 400

        //todo - gfm - 5/20/16 -
        call /internal/global/satellite/{channel}
        this can be async, does not need to succeed for the creation request to complete
        Also needs to create/update replication for the channels

        //todo - gfm - 5/20/16 - can this use the normal replication endpoint?
        for creation, make sure we start replication at the creation time of the channel

        */
        return channel;
    }

    @PUT
    @Path("/satellite/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSatellite(@PathParam("channel") String channelName, String json) throws Exception {
        //todo - gfm - 5/20/16 - make sure master is not set...
        return channelResource.createChannel(channelName, json);
    }
}
