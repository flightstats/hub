package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
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

    private TagEarliestResource tagEarliestResource = HubProvider.getInstance(TagEarliestResource.class);
    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    public Response getEarliest(@PathParam("channel") String channel,
                                @QueryParam("stable") @DefaultValue("true") boolean stable,
                                @QueryParam("trace") @DefaultValue("false") boolean trace,
                                @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagEarliestResource.getEarliest(tag, stable, trace, uriInfo);
        }
        DirectionQuery query = getDirectionQuery(channel, 1, stable, channelService);
        Collection<ContentKey> keys = channelService.getKeys(query);
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
                                     @QueryParam("tag") String tag,
                                     @HeaderParam("Accept") String accept) {
        if (tag != null) {
            return tagEarliestResource.getEarliestCount(tag, count, stable, bulk, batch, trace, accept, uriInfo);
        }
        DirectionQuery query = getDirectionQuery(channel, count, stable, channelService);
        SortedSet<ContentKey> keys = channelService.getKeys(query);
        if (bulk || batch) {
            return BulkBuilder.build(keys, channel, channelService, uriInfo, accept);
        } else {
            return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, false, trace);
        }
    }

    public static DirectionQuery getDirectionQuery(String channel, int count, boolean stable, ChannelService channelService) {
        long ttlDays = channelService.getCachedChannelConfig(channel).getTtlDays();
        DateTime earliestTime = TimeUtil.getEarliestTime((int) ttlDays);
        ContentKey limitKey = new ContentKey(earliestTime, "0");

        return DirectionQuery.builder()
                .channelName(channel)
                .contentKey(limitKey)
                .next(true)
                .stable(stable)
                .ttlDays(ttlDays)
                .count(count)
                .build();
    }


}
