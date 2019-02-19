package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Optional;
import java.util.SortedSet;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@SuppressWarnings("WeakerAccess")
@Path("/tag/{tag}/latest")
public class TagLatestResource {

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private TagService tagService = HubProvider.getInstance(TagService.class);

    @GET
    public Response getLatest(@PathParam("tag") String tag,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                              @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                              @Context UriInfo uriInfo) {
        Optional<ChannelContentKey> latest = tagService.getLatest(getQuery(tag, stable, location, epoch));
        if (latest.isPresent()) {
            URI uri = uriInfo.getBaseUriBuilder()
                    .path(latest.get().toUrl())
                    .queryParam("tag", tag)
                    .build();
            return Response.status(SEE_OTHER).location(uri).build();
        }
        return Response.status(NOT_FOUND).build();
    }

    private DirectionQuery getQuery(String tag, boolean stable, String location, String epoch) {
        return DirectionQuery.builder()
                .tagName(tag)
                .next(false)
                .stable(stable)
                .count(1)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .build();
    }

    @GET
    @Path("/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestCount(@PathParam("tag") String tag,
                                   @PathParam("count") int count,
                                   @QueryParam("stable") @DefaultValue("true") boolean stable,
                                   @QueryParam("batch") @DefaultValue("false") boolean batch,
                                   @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                   @QueryParam("trace") @DefaultValue("false") boolean trace,
                                   @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                   @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                   @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                                   @HeaderParam("Accept") String accept,
                                   @Context UriInfo uriInfo) {
        Optional<ChannelContentKey> latest = tagService.getLatest(getQuery(tag, stable, location, epoch));
        if (!latest.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .startKey(latest.get().getContentKey())
                .next(false)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(count - 1)
                .build();
        SortedSet<ChannelContentKey> keys = tagService.getKeys(query);
        keys.add(latest.get());
        if (bulk || batch) {
            //todo - gfm -
            return BulkBuilder.buildTag(tag, keys, tagService.getChannelService(), uriInfo, accept);
        }
        return LinkBuilder.directionalTagResponse(tag, keys, count, query, mapper, uriInfo, true, trace, Order.isDescending(order));
    }

}
