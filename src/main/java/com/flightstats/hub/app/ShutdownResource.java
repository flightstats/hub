package com.flightstats.hub.app;

import com.flightstats.hub.health.HubHealthCheck;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.concurrent.Executors;

/**
 * ShutdownResource should only be called from the node's instance by the upstart prestop.sh script
 */
@Path("/shutdown")
public class ShutdownResource {

    private final static Logger logger = LoggerFactory.getLogger(ShutdownResource.class);

    @Inject
    HubHealthCheck healthCheck;

    @POST
    public Response shutdown() {
        if (healthCheck.isShuttingDown()) {
            return Response.ok().build();
        }
        logger.warn("shutting down!");
        //this call will get the node removed from the Load Balancer
        healthCheck.shutdown();
        //wait until it's likely the node is removed from the Load Balancer
        int shutdown_delay_seconds = HubProperties.getProperty("app.shutdown_delay_seconds", 5);
        Sleeper.sleep(shutdown_delay_seconds * 1000);
        //after the node isn't getting new requests, stop everything that needs a clean kill
        HubServices.preStop();

        logger.warn("completed shutdown tasks");
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
        return Response.ok().build();
    }
}
