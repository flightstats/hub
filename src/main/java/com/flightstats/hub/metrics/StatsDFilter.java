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
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();
    private DataDogWhitelist dataDogWhitelist;

    @Inject
    public StatsDFilter(DataDogWhitelist dataDogWhitelist) {
        this.dataDogWhitelist = dataDogWhitelist;
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, 8124);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, 8125);
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
