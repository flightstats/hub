package com.flightstats.hub.health;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

@Builder
@Getter
@ToString
public class HealthStatus {
    private final boolean healthy;
    private final String description;
}
