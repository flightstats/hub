package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.StatsDClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Singleton
public class StatsDFilter {
    private StatsDClient statsDClient;
    private StatsDClient dataDogClient;
    private List<String> predicates = Arrays.asList("", "");

    @Inject
    public StatsDFilter(
            StatsDClient statsDClient,
            StatsDClient dataDogClient
            ) {
        this.statsDClient = statsDClient;
        this.dataDogClient = dataDogClient;
    }
    
    public List<StatsDClient> getFilteredClients(String metric) {
        List <StatsDClient> clients = Arrays.asList(statsDClient, dataDogClient);
        boolean matched = match.test(metric);
        return matched ? clients : Collections.singletonList(statsDClient);
    }

    private Predicate<String> match = (metric) -> predicates
            .stream()
            .anyMatch(predicate -> predicate.equals(metric));

}
