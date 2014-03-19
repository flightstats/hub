package com.flightstats.hub.app;

import com.google.common.util.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Services is the class to register for startup and shutdown hooks
 */
public class HubServices {
    private final static Logger logger = LoggerFactory.getLogger(HubServices.class);

    private static List<Service> services = new ArrayList<>();


    public static void register(Service service) {
        //todo - gfm - 2/10/14 - should this limit duplicate scheduling?
        logger.info("registering " + service.getClass().getName());
        services.add(service);
    }

    public static void startAll() {
        try {
            for (Service service : services) {
                logger.info("starting service " + service.getClass().getName());
                service.startAsync();
                service.awaitRunning();
                logger.info("running service " + service.getClass().getName());
            }
        } catch (Exception e) {
            logger.error("unable to start services, exiting", e);
            System.exit(-1);
        }
    }

    public static void stopAll() {
        //todo - gfm - 2/10/14 - what do we need to do here?
    }
}
