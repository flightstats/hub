package com.flightstats.hub.app;

import com.google.common.util.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Services is the class to register for startup and shutdown hooks
 */
public class HubServices {
    private final static Logger logger = LoggerFactory.getLogger(HubServices.class);
    private static Map<TYPE, List<Service>> serviceMap = new HashMap<>();

    static {
        for (TYPE type : TYPE.values()) {
            serviceMap.put(type, new ArrayList<>());
        }
    }

    public static void registerPreStop(Service service) {
        register(service, TYPE.PRE_STOP, TYPE.PRE_START);
    }

    public static void register(Service service) {
        register(service, TYPE.PRE_START);
    }

    public static void register(Service service, TYPE... types) {
        for (TYPE type : types) {
            logger.info("registering " + service.getClass().getName() + " for " + type);
            serviceMap.get(type).add(service);
        }
    }

    public static void start(TYPE type) {
        try {
            for (Service service : serviceMap.get(type)) {
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
        for (TYPE type : TYPE.values()) {
            stop(serviceMap.get(type));
        }
    }

    public static void preStop() {
        stop(serviceMap.get(TYPE.PRE_STOP));
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

    public enum TYPE {
        PRE_STOP,
        PRE_START,
        POST_START,
        STOP
    }
}
