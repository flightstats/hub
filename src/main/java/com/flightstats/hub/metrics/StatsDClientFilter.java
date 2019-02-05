package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
public class StatsDClientFilter {

    private static final Logger logger = LoggerFactory.getLogger(StatsDClientFilter.class);
    private StatsDBaseClient statsDBaseClient;
    private StatsDBaseClient dataDogClient;

    @Inject
    public StatsDClientFilter(
            StatsDBaseClient statsDBaseClient,
            StatsDBaseClient dataDogClient
            ) {
        this.statsDBaseClient = statsDBaseClient;
        this.dataDogClient = dataDogClient;
//        HubServices.register(new DelegatingMetricsServiceInitial(), HubServices.TYPE.BEFORE_HEALTH_CHECK);
    }

    private List<StatsDBaseClient> getFilteredServices(String matcher) {
        List<String> predicates = Arrays.asList("", "");
        List <StatsDBaseClient> clients = Arrays.asList(statsDBaseClient, dataDogClient);
        boolean anyMatch = predicates
                .stream()
                .anyMatch(predicate -> predicate.equals(matcher));
        return anyMatch ? clients : Collections.singletonList(statsDBaseClient);
    }
}
