package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.model.ChannelContentKey;
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

@SuppressWarnings("WeakerAccess")
@Path("/tag/{tag: .*}/latest")
public class TagLatestResource {

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private TagService tagService = HubProvider.getInstance(TagService.class);

    @GET
    public Response getLatest(@PathParam("tag") String tag,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace, @Context UriInfo uriInfo) {
        Optional<ChannelContentKey> latest = tagService.getLatest(tag, stable, trace);
        if (latest.isPresent()) {
            URI uri = uriInfo.getBaseUriBuilder()
                    .path(latest.get().toUrl())
                    .queryParam("tag", tag)
                    .build();
            return Response.status(SEE_OTHER).location(uri).build();
        }
        return Response.status(NOT_FOUND).build();
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
                                   @HeaderParam("Accept") String accept, @Context UriInfo uriInfo) {
        Optional<ChannelContentKey> latest = tagService.getLatest(tag, stable, trace);
        if (!latest.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .contentKey(latest.get().getContentKey())
                .next(false)
                .stable(stable)
                .count(count - 1)
                .build();
        SortedSet<ChannelContentKey> keys = tagService.getKeys(query);
        keys.add(latest.get());
        if (bulk || batch) {
            return BulkBuilder.buildTag(tag, keys, tagService.getChannelService(), uriInfo, accept);
        }
        return LinkBuilder.directionalTagResponse(tag, keys, count, query, mapper, uriInfo, true, trace);
    }

}
