package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import javax.inject.Inject;
import com.google.inject.Provider;

public class StatsDReporterProvider implements Provider<StatsdReporter> {
    private final StatsDFilter statsDFilter;
    private final DatadogMetricsProperties datadogMetricsProperties;
    private final MetricsProperties metricsProperties;


    @Inject
    StatsDReporterProvider(
            StatsDFilter statsDFilter,
            DatadogMetricsProperties datadogMetricsProperties,
            MetricsProperties metricsProperties) {
        this.statsDFilter = statsDFilter;
        this.datadogMetricsProperties = datadogMetricsProperties;
        this.metricsProperties = metricsProperties;
    }

    @Override
    public StatsdReporter get() {
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsProperties);
        DataDogHandler dataDogHandler = new DataDogHandler(datadogMetricsProperties, metricsProperties);
        return new StatsdReporter(statsDFilter, statsDFormatter, dataDogHandler);
    }

}
