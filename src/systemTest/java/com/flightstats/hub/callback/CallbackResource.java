package com.flightstats.hub.callback;

import com.flightstats.hub.model.WebhookCallbackRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("/callback")
public class CallbackResource {

    private CallbackCache callbackCache;

    @Inject
    public CallbackResource(CallbackCache callbackCache) {
        this.callbackCache = callbackCache;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response create(WebhookCallbackRequest webhookCallbackRequest) {
        log.info("Callback request received {} ", webhookCallbackRequest);
        this.callbackCache.put(webhookCallbackRequest);
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/{webhookName}")
    @Produces(MediaType.TEXT_PLAIN)
    @SneakyThrows
    public Response get(@PathParam("webhookName") String webhookName) {

        if (this.callbackCache.contains(webhookName)) {
            return Response
                    .ok(this.callbackCache.get(webhookName).toString())
                    .build();
        } else {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }
    }

}