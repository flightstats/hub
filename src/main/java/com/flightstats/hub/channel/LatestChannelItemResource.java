package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

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
    public Response getLatest(@PathParam("channelName") String channelName,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace) {
        Optional<ContentKey> latest = channelService.getLatest(channelName, stable, trace);
        if (latest.isPresent()) {
            return Response.status(SEE_OTHER)
                    .location(URI.create(uriInfo.getBaseUri() + "channel/" + channelName + "/" + latest.get().toUrl()))
                    .build();
        } else {
            return Response.status(NOT_FOUND).build();
        }
    }

}
