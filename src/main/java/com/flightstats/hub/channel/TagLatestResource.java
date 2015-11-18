package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.model.ChannelContentKey;
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

@Path("/tag/{tag: .*}/latest")
public class TagLatestResource {

    @Inject
    private UriInfo uriInfo;
    @Inject
    private TagService tagService;
    @Inject
    private ObjectMapper mapper;

    @GET
    public Response getLatest(@PathParam("tag") String tag,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace) {
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
                                   @QueryParam("trace") @DefaultValue("false") boolean trace) {
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
        Collection<ChannelContentKey> keys = tagService.getKeys(query);
        keys.add(latest.get());
        if (batch) {
            return MultiPartBatchBuilder.buildTag(tag, keys, tagService.getChannelService(), uriInfo);
        }
        return LinkBuilder.directionalTagResponse(tag, keys, count, query, mapper, uriInfo, true);
    }

}
