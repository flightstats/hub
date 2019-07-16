package com.flightstats.hub.dao.aws;

import com.flightstats.hub.rest.PATCH;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Slf4j
@Path("/internal/max-items")
public class MaxItemsEnforcerResource {

    private final MaxItemsEnforcer maxItemsEnforcer;

    @Inject
    public MaxItemsEnforcerResource(MaxItemsEnforcer maxItemsEnforcer) {
        this.maxItemsEnforcer = maxItemsEnforcer;
    }

    @PATCH
    @Path("/{channel}")
    public Response enforce(@PathParam("channel") String channel) {
        try {
            maxItemsEnforcer.updateMaxItems(channel);
            return ok().build();
        } catch (Exception e) {
            log.warn("unable to complete max items enforcer for " + channel, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getStackTrace()).build();
        }

    }

}
