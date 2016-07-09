package com.flightstats.hub.webhook;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * The former name of webhooks was 'group callback'.  Maybe this can go away someday.
 */
@SuppressWarnings("WeakerAccess")
@Path("/group")
public class GroupResource {

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        return WebhookResource.getWebhooks("groups", uriInfo);
    }

    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        return WebhookResource.get(name, uriInfo);
    }

    @Path("/{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        return WebhookResource.upsert(name, body, uriInfo);
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        return WebhookResource.deleter(name);
    }
}
