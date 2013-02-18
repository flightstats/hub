package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, String> getChannelMetadata(@PathParam("channelName") String channelName) {
        if (!channelDao.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Map<String, String> map = new HashMap<>();
        map.put("name", channelName);
        return map;
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Linked<ValueInsertionResult> insertValue(@PathParam("channelName") String channelName, byte[] data) {
        if (!channelDao.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        ValueInsertionResult insertionResult = channelDao.insert(channelName, data);
        URI channelUri = uriInfo.getRequestUri();
        URI payloadUri = URI.create(channelUri.toString() + "/" + insertionResult.getId().toString());
        return linked(insertionResult)
                .withLink("channel", channelUri)
                .withLink("self", payloadUri)
                .build();
    }

}
