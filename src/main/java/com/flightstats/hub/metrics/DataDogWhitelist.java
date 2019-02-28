package com.flightstats.hub.metrics;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DataDogWhitelist {
    List<String> whitelist;
}
