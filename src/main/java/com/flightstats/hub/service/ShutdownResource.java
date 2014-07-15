package com.flightstats.hub.service;

import com.flightstats.hub.app.HubServices;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/shutdown")
public class ShutdownResource {

    private final static Logger logger = LoggerFactory.getLogger(ShutdownResource.class);

    @Inject
    HubHealthCheck healthCheck;

    @POST
    public Response shutdown() {
        logger.warn("shutting down!");
        healthCheck.shutdown();

        HubServices.preStopAll();

        logger.warn("completed shutdown tasks");
        return Response.ok().build();
    }
}
