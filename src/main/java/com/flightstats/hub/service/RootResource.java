package com.flightstats.hub.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.SEE_OTHER;

/**
 * Convenience Resource to prevent 404s.
 */
@Path("/")
public class RootResource {

	@GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response getChannels(@Context UriInfo uriInfo) {

        /*
        todo - gfm - 6/20/14 - Apparently, some clients were relying on the redirect behavior
        Linked.Builder<?> links = Linked.justLinks();
        links.withLink("self", uriInfo.getRequestUri());
        links.withLink("channel", uriInfo.getRequestUri() + "channel");
        links.withLink("replication", uriInfo.getRequestUri() + "replication");
        links.withLink("group", uriInfo.getRequestUri() + "group");
        links.withLink("tag", uriInfo.getRequestUri() + "tag");
        links.withLink("health", uriInfo.getRequestUri() + "health");
        return Response.ok(links.build()).build();
        */
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        builder.location(URI.create(uriInfo.getRequestUri().toString() + "channel"));
        return builder.build();
	}

}
