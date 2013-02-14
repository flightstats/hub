package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * This resource represents a single value stored in the DataHub.
 */
@Path("/channel/{channelName: .*}/{id}")
public class ChannelContentResource {

    private final ChannelDao channelDao;

    @Inject
    public ChannelContentResource(ChannelDao channelDao) {
        this.channelDao = channelDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] getValue(@PathParam("channelName") String channelName, @PathParam("id") UUID id) {
        byte[] result = channelDao.getValue(channelName, id);
        if (result == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return result;
    }


}
