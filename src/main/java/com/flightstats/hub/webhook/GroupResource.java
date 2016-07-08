package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProvider;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The former name of webhooks was 'group callback'.  Maybe this can go away someday.
 */
@SuppressWarnings("WeakerAccess")
@Path("/group")
public class GroupResource {

    private WebhookResource webhookResource = HubProvider.getInstance(WebhookResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        return webhookResource.getWebhooks("groups");
    }

    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        return webhookResource.get(name);
    }

    @Path("/{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        return webhookResource.upsert(name, body);
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        return webhookResource.delete(name);
    }
}
