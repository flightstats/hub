package com.flightstats.hub.service;

import com.flightstats.jerseyguice.jetty.health.HealthCheck;

public class MemoryHealthCheck implements HealthCheck {

    @Override
    public boolean isHealthy() {
        return true;
    }
}
