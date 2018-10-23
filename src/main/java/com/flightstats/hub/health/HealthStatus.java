package com.flightstats.hub.health;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class HealthStatus {

    private final boolean healthy;
    private final String description;

}
