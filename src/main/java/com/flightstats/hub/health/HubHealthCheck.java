package com.flightstats.hub.health;

import java.util.concurrent.atomic.AtomicBoolean;

public class HubHealthCheck {

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public HealthStatus getStatus() {
        HealthStatus.HealthStatusBuilder builder = HealthStatus.builder();
        if (shutdown.get()) {
            return builder.healthy(false).description("SHUTTING DOWN").build();
        }
        return builder.healthy(true).description("OK").build();
    }

    public void shutdown() {
        shutdown.set(true);
    }

}
