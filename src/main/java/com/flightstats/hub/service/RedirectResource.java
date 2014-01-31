package com.flightstats.hub.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.SEE_OTHER;

/**
 * Convenience Resource to prevent 404s.
 */
@Path("/")
public class RedirectResource {

	@GET
	public Response getChannels(@Context UriInfo uriInfo) {
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        builder.location(URI.create(uriInfo.getRequestUri().toString() + "channel"));
        return builder.build();
	}

}
