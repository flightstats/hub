package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Singleton
public class StatsDFilter {
    private final static String clientPrefix = "hub";
    private final static String clientHost = "localhost";
    private MetricsConfig metricsConfig;
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();
    private DataDogWhitelist dataDogWhitelist;

    @Inject
    public StatsDFilter(DataDogWhitelist dataDogWhitelist,
                        MetricsConfig metricsConfig) {
        this.dataDogWhitelist = dataDogWhitelist;
        this.metricsConfig = metricsConfig;
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        int statsdPort = metricsConfig.getStatsdPort();
        int dogstatsdPort = metricsConfig.getDogstatsdPort();
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, statsdPort);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, dogstatsdPort);
    }

    private Function<Boolean, List<StatsDClient>> clientList = (bool) -> bool ?
            Arrays.asList(statsDClient, dataDogClient) :
            Collections.singletonList(statsDClient);

    private Function<String, Boolean> matcherExists = (matcher) ->  matcher != null && !matcher.equals("");

    private Function<String, Boolean> match = (metric) -> dataDogWhitelist
            .getWhitelist()
            .stream()
            .anyMatch(matcher -> matcherExists.apply(matcher) && matcher.equals(metric));
    
    List<StatsDClient> getFilteredClients(String metric) {
        return clientList.apply(match.apply(metric));
    }

    List<StatsDClient> getAllClients() { return clientList.apply(true); }

}
