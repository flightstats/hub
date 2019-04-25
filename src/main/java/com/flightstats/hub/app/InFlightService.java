package com.flightstats.hub.app;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Slf4j
public class InFlightService {

    private final AtomicInteger inFlight = new AtomicInteger();

    private AppProperty appProperty;

    @Inject
    public InFlightService(AppProperty appProperty) {
        this.appProperty = appProperty;
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
        Integer shutdown_wait_millis = appProperty.getShutdownWaitTimeInMillis();

        log.info("waiting for {} in-flight to complete in {} milliseconds",
                inFlight.get(), shutdown_wait_millis);
        long start = System.currentTimeMillis();

        while (inFlight.get() > 0) {
            log.info("still waiting for in-flight to complete " + inFlight.get());
            Sleeper.sleep(1000);
            if (System.currentTimeMillis() > (start + shutdown_wait_millis)) {
                break;
            }
        }

        log.info("completed waiting for in-flight to complete {} after {} millis ",
                inFlight.get(), (System.currentTimeMillis() - start));
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
