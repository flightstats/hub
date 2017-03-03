package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.SortedSet;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel}/earliest")
public class ChannelEarliestResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelEarliestResource.class);

    @Context
    private UriInfo uriInfo;

    private final static TagEarliestResource tagEarliestResource = HubProvider.getInstance(TagEarliestResource.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    public Response getEarliest(@PathParam("channel") String channel,
                                @QueryParam("stable") @DefaultValue("true") boolean stable,
                                @QueryParam("trace") @DefaultValue("false") boolean trace,
                                @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagEarliestResource.getEarliest(tag, stable, trace, location, epoch, uriInfo);
        }
        DirectionQuery query = getDirectionQuery(channel, 1, stable, location, epoch);
        Collection<ContentKey> keys = channelService.query(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        } else {
            return Response.status(SEE_OTHER)
                    .location(URI.create(uriInfo.getBaseUri() + "channel/" + channel + "/" + keys.iterator().next().toUrl()))
                    .build();
        }
    }

    @GET
    @Path("/{count}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
    public Response getEarliestCount(@PathParam("channel") String channel,
                                     @PathParam("count") int count,
                                     @QueryParam("stable") @DefaultValue("true") boolean stable,
                                     @QueryParam("trace") @DefaultValue("false") boolean trace,
                                     @QueryParam("batch") @DefaultValue("false") boolean batch,
                                     @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                     @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                     @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                     @QueryParam("tag") String tag,
                                     @HeaderParam("Accept") String accept) {
        if (tag != null) {
            return tagEarliestResource.getEarliestCount(tag, count, stable, bulk, batch, trace, location, epoch, accept, uriInfo);
        }
        DirectionQuery query = getDirectionQuery(channel, count, stable, location, epoch);
        SortedSet<ContentKey> keys = channelService.query(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        if (bulk || batch) {
            return BulkBuilder.build(keys, channel, channelService, uriInfo, accept);
        } else {
            return LinkBuilder.directionalResponse(keys, count, query, mapper, uriInfo, false, trace);
        }
    }

    public static DirectionQuery getDirectionQuery(String channel, int count, boolean stable, String location, String epoch) {
        return DirectionQuery.builder()
                .channelName(channel)
                .next(true)
                .stable(stable)
                .count(count)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .build();
    }


}
