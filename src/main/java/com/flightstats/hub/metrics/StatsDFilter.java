package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
public class StatsDFilter {
    private final static String clientPrefix = "hub";
    private final static String clientHost = "localhost";
    private MetricsConfig metricsConfig;
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();

    @Inject
    public StatsDFilter(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        int statsdPort = metricsConfig.getStatsdPort();
        int dogstatsdPort = metricsConfig.getDogstatsdPort();
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, statsdPort);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, dogstatsdPort);
    }

    List<StatsDClient> getFilteredClients(boolean secondaryReporting) {
        return secondaryReporting ?
                Arrays.asList(statsDClient, dataDogClient) :
                Collections.singletonList(statsDClient);
    }

}
