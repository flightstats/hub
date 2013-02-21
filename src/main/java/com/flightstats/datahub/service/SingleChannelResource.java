package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents a single channel in the DataHub.
 */
@Path("/channel/{channelName}")
public class SingleChannelResource {

    private final ChannelDao channelDao;
    private final UriInfo uriInfo;

    @Inject
    public SingleChannelResource(ChannelDao channelDao, UriInfo uriInfo) {
        this.channelDao = channelDao;
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Linked<ChannelConfiguration> getChannelMetadata(@PathParam("channelName") String channelName) {
        if (!channelDao.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        ChannelConfiguration config = channelDao.getChannelConfiguration(channelName);
        URI selfUri = uriInfo.getRequestUri();
        URI latestUri = URI.create(selfUri + "/latest");
        return linked(config)
                .withLink("self", selfUri)
                .withLink("latest", latestUri)
                .build();
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Linked<ValueInsertionResult> insertValue(@HeaderParam("Content-Type") String contentType, @PathParam(
            "channelName") String channelName, byte[] data) {
        if (!channelDao.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        ValueInsertionResult insertionResult = channelDao.insert(channelName, contentType, data);
        URI channelUri = uriInfo.getRequestUri();
        URI payloadUri = URI.create(channelUri.toString() + "/" + insertionResult.getKey().toString());
        return linked(insertionResult)
                .withLink("channel", channelUri)
                .withLink("self", payloadUri)
                .build();
    }

}
