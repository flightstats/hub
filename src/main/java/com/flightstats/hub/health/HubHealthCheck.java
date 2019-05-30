package com.flightstats.hub.health;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.spoke.SpokeFinalCheck;
import com.google.common.util.concurrent.AbstractIdleService;
import javax.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Slf4j
public class HubHealthCheck {
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean startup = new AtomicBoolean(true);
    private final AtomicBoolean decommissionedWithinSpoke = new AtomicBoolean(false);
    private final AtomicBoolean decommissionedDoNotRestart = new AtomicBoolean(false);
    @Inject
    private SpokeFinalCheck spokeFinalCheck;

    public HubHealthCheck() {
        HubServices.register(new HealthService(), HubServices.TYPE.PERFORM_HEALTH_CHECK);
    }

    public HealthStatus getStatus() {
        HealthStatus.HealthStatusBuilder builder = HealthStatus.builder();
        if (shutdown.get()) {
            return builder.healthy(false).description("SHUTTING DOWN").build();
        }
        if (startup.get()) {
            return builder.healthy(false).description("Starting up...").build();
        }
        if (decommissionedWithinSpoke.get()) {
            return builder.healthy(true).description("Decommissioned, still serving Spoke queries").build();
        }
        if (decommissionedDoNotRestart.get()) {
            return builder.healthy(false).description("Decommissioned, not serving Spoke queries").build();
        }
        return builder.healthy(true).description("OK").build();
    }

    public void shutdown() {
        shutdown.set(true);
    }

    public boolean isShuttingDown() {
        return shutdown.get();
    }

    public void decommissionWithinSpoke() {
        decommissionedWithinSpoke.set(true);
    }

    public void decommissionedDoNotRestart() {
        decommissionedWithinSpoke.set(false);
        decommissionedDoNotRestart.set(true);
    }

    private class HealthService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            if (!spokeFinalCheck.check()) {
                log.warn("unable to cleanly start!");
                throw new RuntimeException("unable to cleanly start");
            }
            startup.set(false);
        }

        @Override
        protected void shutDown() {
            shutdown();
        }
    }

}
