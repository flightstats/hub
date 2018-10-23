package com.flightstats.hub.webhook;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * The former name of webhooks was 'group callback'.  Maybe this can go away someday.
 */
@Path("/group")
public class GroupResource {

    private final WebhookResource webhookResource;
    @Context
    private UriInfo uriInfo;

    @Inject
    GroupResource(WebhookResource webhookResource) {
        this.webhookResource = webhookResource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        return webhookResource.getWebhooks("groups", uriInfo);
    }

    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        return webhookResource.get(name, uriInfo);
    }

    @Path("/{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        return webhookResource.upsert(name, body, uriInfo);
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        return webhookResource.deleter(name);
    }
}
