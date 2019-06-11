package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class TimeQueryResult {
    private Links _links;
}
