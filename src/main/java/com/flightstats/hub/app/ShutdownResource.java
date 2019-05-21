package com.flightstats.hub.app;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@Path("/shutdown")
public class ShutdownResource {

    private final ShutdownManager shutdownManager;

    @Inject
    public ShutdownResource(ShutdownManager shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    @POST
    public Response shutdown(@Context UriInfo uriInfo) throws Exception {
        return LocalHostOnly.getResponse(uriInfo, () -> this.shutdownManager.shutdown(true));
    }
}
