package com.flightstats.hub.service;

import com.flightstats.hub.model.HealthStatus;

public class MemoryHealthCheck implements HubHealthCheck {

    @Override
    public HealthStatus getStatus() {
        return HealthStatus.builder().healthy(true).description("OK").build();
    }

    @Override
    public void shutdown() {

    }
}
