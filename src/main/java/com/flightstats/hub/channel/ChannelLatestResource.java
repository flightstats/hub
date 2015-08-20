package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel: .*}/latest")
public class ChannelLatestResource {

    @Inject
    private UriInfo uriInfo;
    @Inject
    private ChannelService channelService;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private TagLatestResource tagLatestResource;

    @GET
    public Response getLatest(@PathParam("channel") String channel,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagLatestResource.getLatest(tag, stable, trace);
        }
        Optional<ContentKey> latest = channelService.getLatest(channel, stable, trace);
        if (latest.isPresent()) {
            return Response.status(SEE_OTHER)
                    .location(URI.create(uriInfo.getBaseUri() + "channel/" + channel + "/" + latest.get().toUrl()))
                    .build();
        } else {
            return Response.status(NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{count}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*"})
    public Response getLatestCount(@PathParam("channel") String channel,
                                   @PathParam("count") int count,
                                   @QueryParam("stable") @DefaultValue("true") boolean stable,
                                   @QueryParam("trace") @DefaultValue("false") boolean trace,
                                   @QueryParam("batch") @DefaultValue("false") boolean batch,
                                   @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagLatestResource.getLatestCount(tag, count, stable, trace);
        }
        Optional<ContentKey> latest = channelService.getLatest(channel, stable, trace);
        if (!latest.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(latest.get())
                .next(false)
                .stable(stable)
                .ttlDays(channelService.getCachedChannelConfig(channel).getTtlDays())
                .count(count - 1)
                .build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.getKeys(query);
        keys.add(latest.get());
        if (batch) {
            return MultiPartBuilder.build(keys, channel, channelService, uriInfo);
        } else {
            return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, true);
        }

    }

}
