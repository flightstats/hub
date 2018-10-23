package com.flightstats.hub.channel;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/tag/{tag}/time")
public class TagTimeResource {

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault() {
        return TimeLinkUtil.getDefault(uriInfo);
    }

    @Path("/second")
    @GET
    public Response getSecond(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getSecond(stable, uriInfo);
    }

    @Path("/minute")
    @GET
    public Response getMinute(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getMinute(stable, uriInfo);
    }

    @Path("/hour")
    @GET
    public Response getHour(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getHour(stable, uriInfo);
    }

    @Path("/day")
    @GET
    public Response getDay(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getDay(stable, uriInfo);
    }
}
