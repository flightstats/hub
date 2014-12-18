package com.flightstats.hub.service;

import com.flightstats.hub.app.config.metrics.EventTimed;
import com.flightstats.hub.dao.ChannelService;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/channel/{channelName: .*}/latest")
public class LatestChannelItemResource {

    private final UriInfo uriInfo;
    private final ChannelService channelService;

    @Inject
    public LatestChannelItemResource(UriInfo uriInfo, ChannelService channelService) {
        this.uriInfo = uriInfo;
        this.channelService = channelService;
    }

    @GET
    @EventTimed(name = "channel.ALL.latest.get")
    public Response getLatest(@PathParam("channelName") String channelName) {
        /*Optional<ContentKey> latestId = channelService.findLastUpdatedKey(channelName);
        if (!latestId.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);

        String channelUri = uriInfo.getRequestUri().toString().replaceFirst("/latest$", "");
        ContentKey keyOfLatestItem = latestId.get();
        URI uri = URI.create(channelUri + "/" + keyOfLatestItem.toUrl());
        builder.location(uri);
        return builder.build();*/
        return null;
    }

}
