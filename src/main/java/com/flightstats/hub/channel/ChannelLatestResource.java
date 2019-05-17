package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.Order;
import com.flightstats.hub.util.TimeUtil;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

    private final TagLatestResource tagLatestResource;
    private final ChannelService channelService;
    private final LinkBuilder linkBuilder;
    private final BulkBuilder bulkBuilder;
    private final ContentRetriever contentRetriever;

    @Context
    private UriInfo uriInfo;

    @Inject
    public ChannelLatestResource(TagLatestResource tagLatestResource,
                                 ChannelService channelService,
                                 LinkBuilder linkBuilder,
                                 BulkBuilder bulkBuilder, ContentRetriever contentRetriever) {
        this.tagLatestResource = tagLatestResource;
        this.channelService = channelService;
        this.linkBuilder = linkBuilder;
        this.bulkBuilder = bulkBuilder;
        this.contentRetriever = contentRetriever;
    }

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
        Optional<ContentKey> latest = contentRetriever.getLatest(query);
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
        Optional<ContentKey> latest = contentRetriever.getLatest(latestQuery);
        if (!latest.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        final DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .startKey(latest.get())
                .next(false)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(count - 1)
                .build();
        SortedSet<ContentKey> keys = new TreeSet<>(contentRetriever.query(query));
        keys.add(latest.get());
        return getResponse(channel, count, trace, batch, bulk, accept, query, keys, Order.isDescending(order));
    }

    private Response getResponse(String channel,
                                 int count,
                                 boolean trace,
                                 boolean batch,
                                 boolean bulk,
                                 String accept,
                                 DirectionQuery query,
                                 SortedSet<ContentKey> keys,
                                 boolean descending) {
        if (bulk || batch) {
            return bulkBuilder.build(keys, channel, channelService, uriInfo, accept, descending);
        } else {
            return linkBuilder.directionalResponse(keys, count, query, uriInfo, true, trace, descending);
        }
    }

}
