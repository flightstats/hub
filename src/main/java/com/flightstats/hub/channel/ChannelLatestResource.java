package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel}/latest")
public class ChannelLatestResource {

    @Context
    private UriInfo uriInfo;

    private final static TagLatestResource tagLatestResource = HubProvider.getInstance(TagLatestResource.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    public Response getLatest(@PathParam("channel") String channel,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                              @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                              @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagLatestResource.getLatest(tag, stable, trace, location, epoch, uriInfo);
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .next(false)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(1)
                .build();
        Optional<ContentKey> latest = channelService.getLatest(query);
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
                                   @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                   @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                   @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                                   @QueryParam("tag") String tag,
                                   @HeaderParam("Accept") String accept) {
        if (tag != null) {
            return tagLatestResource.getLatestCount(tag, count, stable, batch, bulk, trace, location, epoch, order, accept, uriInfo);
        }
        DirectionQuery latestQuery = DirectionQuery.builder()
                .channelName(channel)
                .next(false)
                .stable(stable)
                .startKey(new ContentKey(TimeUtil.time(stable), "0"))
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(1)
                .build();
        Optional<ContentKey> latest = channelService.getLatest(latestQuery);
        if (!latest.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .startKey(latest.get())
                .next(false)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(count - 1)
                .build();
        SortedSet<ContentKey> keys = new TreeSet<>(channelService.query(query));
        keys.add(latest.get());
        return getResponse(channel, count, trace, batch, bulk, accept, query, keys, Order.isDescending(order));
    }

    private Response getResponse(String channel, int count, boolean trace, boolean batch, boolean bulk,
                                 String accept, DirectionQuery query, SortedSet<ContentKey> keys, boolean descending) {
        if (bulk || batch) {
            return BulkBuilder.build(keys, channel, channelService, uriInfo, accept, descending);
        } else {
            return LinkBuilder.directionalResponse(keys, count, query, mapper, uriInfo, true, trace, descending);
        }
    }

}
