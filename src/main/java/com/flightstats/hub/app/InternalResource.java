package com.flightstats.hub.app;

import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@SuppressWarnings("WeakerAccess")
@Path("/internal")
public class InternalResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels(@Context UriInfo uriInfo) {
        Linked.Builder<?> links = Linked.linked("Internal APIs may change at any time, and are intended for debugging only.");
        links.withLink("self", uriInfo.getRequestUri());

        links.withRelativeLink("channel", uriInfo);
        links.withRelativeLink("curator", uriInfo);
        links.withRelativeLink("health", uriInfo);
        links.withRelativeLink("shutdown", uriInfo);
        links.withRelativeLink("stacktrace", uriInfo);
        links.withRelativeLink("traces", uriInfo);
        links.withRelativeLink("zookeeper", uriInfo);

        return Response.ok(links.build()).build();
    }
}
