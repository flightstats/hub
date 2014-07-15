package com.flightstats.hub.service;

import com.flightstats.hub.model.HealthStatus;

/**
 *
 */
public interface HubHealthCheck {

    HealthStatus getStatus();

    void shutdown();
}
