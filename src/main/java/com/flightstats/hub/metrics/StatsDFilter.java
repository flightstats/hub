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
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();
    private DataDogWhitelist dataDogWhitelist;

    @Inject
    public StatsDFilter(DataDogWhitelist dataDogWhitelist) {
        this.dataDogWhitelist = dataDogWhitelist;
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        this.statsDClient = new NonBlockingStatsDClient("hub", "localhost", 8124);
        this.dataDogClient = new NonBlockingStatsDClient("hub", "localhost", 8125);
    }

    private Function<Boolean, List<StatsDClient>> clientList = (bool) -> bool ?
            Arrays.asList(statsDClient, dataDogClient) :
            Collections.singletonList(statsDClient);

    private Function<String, Boolean> match = (metric) -> dataDogWhitelist
            .getWhitelist()
            .stream()
            .anyMatch(matcher -> matcher.equals(metric));
    
    List<StatsDClient> getFilteredClients(String metric) {
        return clientList.apply(match.apply(metric));
    }

    List<StatsDClient> getAllClients() { return clientList.apply(true); }

}
