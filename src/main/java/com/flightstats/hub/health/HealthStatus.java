package com.flightstats.hub.health;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class HealthStatus {
    private final boolean healthy;
    private final String description;
}
