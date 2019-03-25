package com.flightstats.hub.spoke;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SpokeStoreConfig {
    SpokeStore type;
    String path;
    int ttlMinutes;
}
