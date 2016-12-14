package com.flightstats.hub.app;

import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Singleton
public class InFlightService {
    private final static Logger logger = LoggerFactory.getLogger(InFlightService.class);
    private final AtomicInteger inFlight = new AtomicInteger();

    public InFlightService() {
        HubServices.registerPreStop(new InFlightServiceShutdown());
    }

    public <X> X inFlight(Supplier<X> supplier) {
        try {
            inFlight.incrementAndGet();
            return supplier.get();
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private void waitForInFlight() {
        Integer shutdown_wait_seconds = HubProperties.getProperty("app.shutdown_wait_seconds", 10);
        logger.info("waiting for in-flight to complete " + inFlight.get());
        long start = System.currentTimeMillis();
        while (inFlight.get() > 0) {
            logger.info("still waiting for in-flight to complete " + inFlight.get());
            Sleeper.sleep(1000);
            if (System.currentTimeMillis() > (start + shutdown_wait_seconds * 1000)) {
                break;
            }
        }
        logger.info("completed waiting for in-flight to complete " + inFlight.get());
    }


    private class InFlightServiceShutdown extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            //do nothing
        }

        @Override
        protected void shutDown() throws Exception {
            waitForInFlight();
        }
    }
}
