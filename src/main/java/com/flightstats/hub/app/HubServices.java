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

    private static List<Service> allServices = new ArrayList<>();
    private static List<Service> preStopServices = new ArrayList<>();

    public static void registerPreStop(Service service) {
        logger.info("registering pre stop" + service.getClass().getName());
        preStopServices.add(service);
        register(service);
    }

    public static void register(Service service) {
        logger.info("registering " + service.getClass().getName());
        allServices.add(service);
    }

    public static void startAll() {
        try {
            for (Service service : allServices) {
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
        stop(allServices);
    }

    public static void preStopAll() {
        stop(preStopServices);
    }

    private static void stop(List<Service> services) {
        for (Service service : services) {
            logger.info("stopping service " + service.getClass().getName());
            service.stopAsync();
        }
        for (Service service : services) {
            await(service);
            logger.info("stopped service " + service.getClass().getName());
        }
    }

    private static void await(Service service) {
        try {
            service.awaitTerminated();
        } catch (Exception e) {
            logger.error("unable to stop service" + service.getClass().getName(), e);
        }
    }
}
