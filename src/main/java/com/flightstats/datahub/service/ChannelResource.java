package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.exception.AlreadyExistsException;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

@Path("/channel")
public class ChannelResource {

    private final ChannelDao channelDao;
    private final UriInfo uriInfo;

    @Inject
    public ChannelResource(UriInfo uriInfo, ChannelDao channelDao) {
        this.channelDao = channelDao;
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getChannels() {
        throw new RuntimeException("Channels metadata is not yet implemented");
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Linked<ChannelConfiguration> createChannel(ChannelCreationRequest channelCreationRequest) {
        if (channelDao.channelExists(channelCreationRequest.getName())) {
            throw new AlreadyExistsException();
        }

        ChannelConfiguration channelConfiguration = channelDao.createChannel(channelCreationRequest.getName(),
                channelCreationRequest.getDescription());

        URI requestUri = uriInfo.getRequestUri();
        URI channelUri = URI.create(requestUri + "/" + channelCreationRequest.getName());
        URI latestUri = URI.create(requestUri + "/" + channelCreationRequest.getName() + "/latest");
        return linked(channelConfiguration)
                .withLink("self", channelUri)
                .withLink("latest", latestUri)
                .build();
    }
}
