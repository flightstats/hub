package com.flightstats.hub.metrics;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
class DataDogWhitelist {
    List<String> whitelist;
}
