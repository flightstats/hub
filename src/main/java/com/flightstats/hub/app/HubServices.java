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
        for (Service service : services) {
            Service.State state = service.startAndWait();
            logger.info("service " + service.getClass().getName() + " " + state);
        }
    }

    public static void stopAll() {
        //todo - gfm - 2/10/14 - what do we need to do here?
    }
}
