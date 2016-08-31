package com.flightstats.hub.app;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@SuppressWarnings("WeakerAccess")
@Path("/shutdown")
public class ShutdownResource {

    @POST
    public Response shutdown(@Context UriInfo uriInfo) throws Exception {
        ShutdownManager manager = HubProvider.getInstance(ShutdownManager.class);
        return LocalHostOnly.getResponse(uriInfo, manager::shutdown);
    }
}
