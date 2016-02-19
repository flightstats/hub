package com.flightstats.hub.app;

import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/internal")
public class InternalResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels(@Context UriInfo uriInfo) {
        Linked.Builder<?> links = Linked.linked("Internal APIs may change at any time, and are intended for debugging only.");
        links.withLink("self", uriInfo.getRequestUri());

        links.withLink("zookeeper", uriInfo.getRequestUri() + "/zookeeper");
        links.withLink("traces", uriInfo.getRequestUri() + "/traces");
        links.withLink("time", uriInfo.getRequestUri() + "/time");

        return Response.ok(links.build()).build();
    }
}
