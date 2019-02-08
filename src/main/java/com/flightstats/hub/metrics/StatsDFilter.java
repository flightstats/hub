package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.StatsDClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Singleton
public class StatsDFilter {
    private StatsDClient statsDClient;
    private StatsDClient dataDogClient;
    private DataDogWhitelist dataDogWhitelist;

    @Inject
    public StatsDFilter(
            StatsDClient statsDClient,
            StatsDClient dataDogClient,
            DataDogWhitelist dataDogWhitelist
            ) {
        this.statsDClient = statsDClient;
        this.dataDogClient = dataDogClient;
        this.dataDogWhitelist = dataDogWhitelist;
    }

    private Function<Boolean, List<StatsDClient>> clientList = (bool) -> bool ?
            Arrays.asList(statsDClient, dataDogClient) :
            Collections.singletonList(statsDClient);

    private Function<String, Boolean> match = (metric) -> dataDogWhitelist
            .getWhitelist()
            .stream()
            .anyMatch(predicate -> predicate.equals(metric));
    
    List<StatsDClient> getFilteredClients(String metric) {
        return clientList.apply(match.apply(metric));
    }

}
