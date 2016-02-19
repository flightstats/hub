package com.flightstats.hub.time;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.TimeUtil;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/internal/time")
public class TimeInternalResource {

    private final static TimeService timeService = HubProvider.getInstance(TimeService.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) {
        ObjectNode root = mapper.createObjectNode();
        root.put("remote", timeService.isRemote());
        Linked.Builder<?> builder = Linked.linked(root);
        builder.withLink("millis", uriInfo.getRequestUri() + "/millis");
        builder.withLink("remote", uriInfo.getRequestUri() + "/remote");
        builder.withLink("local", uriInfo.getRequestUri() + "/local");
        return Response.ok(builder.build()).build();
    }

    @GET
    @Path("/millis")
    public Response getMillis() {
        if (timeService.isRemote()) {
            return Response.status(521).build();
        }
        return Response.ok(TimeUtil.now().getMillis()).build();
    }

    @PUT
    @Path("/remote")
    public Response remote() {
        timeService.setRemote(true);
        return Response.ok().build();
    }

    @GET
    @Path("/remote")
    public Response getRemote() {
        return Response.ok(timeService.getRemoteNow().getMillis()).build();
    }

    @PUT
    @Path("/local")
    public Response ok() {
        timeService.setRemote(false);
        return Response.ok().build();
    }

    @GET
    @Path("/local")
    public Response getLocal() {
        return Response.ok(TimeUtil.now().getMillis()).build();
    }
}
