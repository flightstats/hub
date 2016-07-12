package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.google.common.base.Optional;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.SortedSet;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel}/latest")
public class ChannelLatestResource {

    @Context
    private UriInfo uriInfo;

    private TagLatestResource tagLatestResource = HubProvider.getInstance(TagLatestResource.class);
    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    public Response getLatest(@PathParam("channel") String channel,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagLatestResource.getLatest(tag, stable, trace, uriInfo);
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
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
    public Response getLatestCount(@PathParam("channel") String channel,
                                   @PathParam("count") int count,
                                   @QueryParam("stable") @DefaultValue("true") boolean stable,
                                   @QueryParam("trace") @DefaultValue("false") boolean trace,
                                   @QueryParam("batch") @DefaultValue("false") boolean batch,
                                   @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                   @QueryParam("tag") String tag,
                                   @HeaderParam("Accept") String accept) {
        if (tag != null) {
            return tagLatestResource.getLatestCount(tag, count, stable, batch, bulk, trace, accept, uriInfo);
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
        SortedSet<ContentKey> keys = channelService.getKeys(query);
        keys.add(latest.get());
        if (bulk || batch) {
            return BulkBuilder.build(keys, channel, channelService, uriInfo, accept);
        } else {
            return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, true, trace);
        }

    }

}
