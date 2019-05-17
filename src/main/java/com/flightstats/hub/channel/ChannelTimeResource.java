package com.flightstats.hub.channel;

import com.flightstats.hub.dao.aws.ContentRetriever;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/channel/{channel}/time")
public class ChannelTimeResource {

    private final ContentRetriever contentRetriever;
    private final TimeLinkBuilder timeLinkBuilder;

    @Context
    private UriInfo uriInfo;

    @Inject
    public ChannelTimeResource(ContentRetriever contentRetriever, TimeLinkBuilder timeLinkBuilder) {
        this.contentRetriever = contentRetriever;
        this.timeLinkBuilder = timeLinkBuilder;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault(@PathParam("channel") String channel) {
        if (!contentRetriever.isExistingChannel(channel)) {
            return Response.status(404).build();
        }
        return timeLinkBuilder.getDefault(uriInfo);
    }

    @Path("/second")
    @GET
    public Response getSecond(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return timeLinkBuilder.getSecond(stable, uriInfo);
    }

    @Path("/minute")
    @GET
    public Response getMinute(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return timeLinkBuilder.getMinute(stable, uriInfo);
    }

    @Path("/hour")
    @GET
    public Response getHour(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return timeLinkBuilder.getHour(stable, uriInfo);
    }

    @Path("/day")
    @GET
    public Response getDay(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return timeLinkBuilder.getDay(stable, uriInfo);
    }

}
