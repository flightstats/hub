package com.flightstats.hub.callback;

import com.flightstats.hub.callback.model.RequestObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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

    private final CacheObject cacheObject = new CacheObject();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response create(RequestObject requestObject) {
        log.info("Callback request received {} ", requestObject);

        this.cacheObject.put(requestObject);

        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/{webhookname}")
    @Produces(MediaType.TEXT_PLAIN)
    @SneakyThrows
    public Response get(@PathParam("webhookname") String webhookName) {

        if (this.cacheObject.contains(webhookName)) {
            return Response
                    .ok(this.cacheObject.get(webhookName).toString())
                    .build();
        } else {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        }
    }
}