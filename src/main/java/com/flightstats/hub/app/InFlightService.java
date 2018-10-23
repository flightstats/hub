package com.flightstats.hub.app;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class InFlightService {
    private final static Logger logger = LoggerFactory.getLogger(InFlightService.class);
    private final AtomicInteger inFlight = new AtomicInteger();
    private final HubProperties hubProperties;

    @Inject
    public InFlightService(HubProperties hubProperties) {
        this.hubProperties = hubProperties;
        HubServices.registerPreStop(new InFlightServiceShutdown());
    }

    public <X> X inFlight(Throwing.Supplier<X> supplier) {
        try {
            inFlight.incrementAndGet();
            return Errors.rethrow().wrap(supplier).get();
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private void waitForInFlight() {
        Integer shutdown_wait_seconds = hubProperties.getProperty("app.shutdown_wait_seconds", 180);
        logger.info("waiting for " + inFlight.get() + " in-flight to complete in " + shutdown_wait_seconds + " seconds");
        long start = System.currentTimeMillis();
        while (inFlight.get() > 0) {
            logger.info("still waiting for in-flight to complete " + inFlight.get());
            Sleeper.sleep(1000);
            if (System.currentTimeMillis() > (start + shutdown_wait_seconds * 1000)) {
                break;
            }
        }
        logger.info("completed waiting for in-flight to complete " + inFlight.get()
                + " after " + (System.currentTimeMillis() - start) + " millis");
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
