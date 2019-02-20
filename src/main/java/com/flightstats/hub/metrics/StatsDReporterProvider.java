package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class StatsDReporterProvider implements Provider<StatsdReporter> {
    private StatsDFilter statsDFilter;
    private MetricsConfig metricsConfig;

    @Inject
    StatsDReporterProvider(
            StatsDFilter statsDFilter,
            MetricsConfig metricsConfig
    ) {
        this.statsDFilter = statsDFilter;
        this.metricsConfig = metricsConfig;
    }

    @Override public StatsdReporter get() {
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        DataDogHandler dataDogHandler = new DataDogHandler(metricsConfig);
        return new StatsdReporter(statsDFilter, statsDFormatter, dataDogHandler);
    }

}
