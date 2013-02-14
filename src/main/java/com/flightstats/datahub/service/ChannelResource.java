package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.ValueInsertedResponse;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        ChannelConfiguration channelConfiguration = channelDao.createChannel(channelCreationRequest.getName());
        URI requestUri = uriInfo.getRequestUri();
        URI channelUri = URI.create(requestUri + "/" + channelCreationRequest.getName());
        return linked(channelConfiguration)
                .withLink("self", channelUri)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{channelName: .*}")
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
    @Path("/{channelName: .*}")
    public Linked<ValueInsertedResponse> insertValue(@PathParam("channelName") String channelName, byte[] data) {
        UUID uid = channelDao.insert(channelName, data);
        URI channelUri = uriInfo.getRequestUri();
        URI payloadUri = URI.create(channelUri.toString() + "/" + uid.toString());
        return linked(new ValueInsertedResponse(uid))
                .withLink("channel", channelUri)
                .withLink("self", payloadUri)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{channelName: .*}/{id}")
    public byte[] getValue(@PathParam("channelName") String channelName, @PathParam("id") UUID id) {
        byte[] result = channelDao.getValue(channelName, id);
        if (result == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return result;
    }
}
