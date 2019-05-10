package com.flightstats.hub.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Services is the class to register for startup and shutdown hooks
 */
public class HubServices {
    private final static Logger logger = LoggerFactory.getLogger(HubServices.class);
    private final static Map<TYPE, List<Service>> serviceMap = new ConcurrentHashMap<>();

    static {
        for (TYPE type : TYPE.values()) {
            serviceMap.put(type, new ArrayList<>());
        }
    }

    public static void registerPreStop(Service service) {
        register(service, TYPE.BEFORE_HEALTH_CHECK, TYPE.PRE_STOP);
    }

    public static void register(Service service) {
        register(service, TYPE.BEFORE_HEALTH_CHECK);
    }

    public static void register(Service service, TYPE... types) {
        for (TYPE type : types) {
            logger.info("registering " + service.getClass().getName() + " for " + type);
            serviceMap.get(type).add(service);
        }
    }

    public static void start(TYPE type) {
        try {
            List<Service> serviceList = new ArrayList<>();
            synchronized (serviceMap) {
                serviceList.addAll(serviceMap.get(type));
            }
            for (Service service : serviceList) {
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

    @VisibleForTesting
    public static Map<TYPE, List<Service>> getServices() {
        return serviceMap;
    }

    @VisibleForTesting
    public static void clear() {
        serviceMap.forEach((type, list) -> list.clear());
    }

    static void stopAll() {
        List<Service> allServices = new ArrayList<>();
        for (TYPE type : TYPE.values()) {
            allServices.addAll(serviceMap.get(type));
        }
        stop(allServices);
    }

    static void preStop() {
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
            service.awaitTerminated(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("unable to stop service" + service.getClass().getName(), e);
        }
    }

    public enum TYPE {
        /**
         * TYPEs are in execution order
         */
        BEFORE_HEALTH_CHECK,
        PERFORM_HEALTH_CHECK,
        AFTER_HEALTHY_START,
        PRE_STOP,
        STOP
    }
}
