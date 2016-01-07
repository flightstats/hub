package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/tag/{tag: .*}/time")
public class TagTimeResource {

    private final static Logger logger = LoggerFactory.getLogger(TagTimeResource.class);

    @Context
    private UriInfo uriInfo;

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

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
