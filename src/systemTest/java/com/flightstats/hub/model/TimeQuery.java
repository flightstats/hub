package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class TimeQuery {
    private Links _links;
}
