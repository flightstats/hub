package com.flightstats.hub.app;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@SuppressWarnings("WeakerAccess")
@Path("/shutdown")
public class ShutdownResource {

    @POST
    public Response shutdown() throws Exception {
        HubProvider.getInstance(ShutdownManager.class).shutdown();
        return Response.ok().build();
    }
}
