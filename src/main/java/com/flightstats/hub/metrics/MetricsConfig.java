package com.flightstats.hub.metrics;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Value;

@Singleton
@Builder
@Value
public class MetricsConfig {
    String appVersion;
    String clusterTag;
    boolean enabled;
    String env;
    String hostTag;
    String influxdbDatabaseName;
    String influxdbHost;
    String influxdbPass;
    int influxdbPort;
    String influxdbProtocol;
    String influxdbUser;
    int reportingIntervalSeconds;
    String role;
    String team;
}
