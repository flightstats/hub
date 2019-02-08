package com.flightstats.hub.metrics;

import com.google.inject.Provider;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

public class StatsDReporterProvider implements Provider {
    private static final StatsDClient dogstatsd = new NonBlockingStatsDClient("hub", "localhost", 8125);
    private static final StatsDClient statsDClient = new NonBlockingStatsDClient("hub", "localhost", 8124);
    private DataDogWhitelist whitelist;
    private MetricsConfig metricsConfig;

    public StatsDReporterProvider(
            DataDogWhitelist whitelist,
            MetricsConfig metricsConfig
    ) {
        this.whitelist = whitelist;
        this.metricsConfig = metricsConfig;
    }

    @Override public StatsDHandlers get() {
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        StatsDFilter statsDFilter = new StatsDFilter(dogstatsd, statsDClient, whitelist);
        DataDogClient dataDogClient = new DataDogClient(metricsConfig);
        return new StatsDHandlers(statsDFilter, statsDFormatter, dataDogClient);
    }

}
