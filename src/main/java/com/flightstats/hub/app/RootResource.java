package com.flightstats.hub.app;

import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class RootResource {

	@GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response getChannels(@Context UriInfo uriInfo) {
        Linked.Builder<?> links = Linked.justLinks();
        links.withLink("self", uriInfo.getRequestUri());
        links.withLink("documentation", "https://github.com/flightstats/hubv2");
        links.withLink("channel", uriInfo.getRequestUri() + "channel");
        links.withLink("group", uriInfo.getRequestUri() + "group");
        links.withLink("tag", uriInfo.getRequestUri() + "tag");
        links.withLink("health", uriInfo.getRequestUri() + "health");
        return Response.ok(links.build()).build();
    }

}
