package com.flightstats.hub.metrics;

import com.google.inject.Provider;

public class StatsDReporterProvider implements Provider {
    private StatsDFilter statsDFilter;
    private MetricsConfig metricsConfig;

    StatsDReporterProvider(
            StatsDFilter statsDFilter,
            MetricsConfig metricsConfig
    ) {
        this.statsDFilter = statsDFilter;
        this.metricsConfig = metricsConfig;
    }

    @Override public StatsDHandlers get() {
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        DataDogHandler dataDogHandler = new DataDogHandler(metricsConfig);
        return new StatsDHandlers(statsDFilter, statsDFormatter, dataDogHandler);
    }

}
