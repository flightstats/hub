package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.GrafanaMetricsProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import javax.inject.Inject;
import com.google.inject.Provider;

public class StatsDReporterProvider implements Provider<StatsdReporter> {
    private final StatsDFilter statsDFilter;
    private final DatadogMetricsProperties datadogMetricsProperties;
    private final MetricsProperties metricsProperties;
   // private final GrafanaMetricsProperties grafanaMetricsProperties;


    @Inject
    StatsDReporterProvider(
            StatsDFilter statsDFilter,
            DatadogMetricsProperties datadogMetricsProperties,
            MetricsProperties metricsProperties) {
        this.statsDFilter = statsDFilter;
        this.datadogMetricsProperties = datadogMetricsProperties;
        this.metricsProperties = metricsProperties;
       // this.grafanaMetricsProperties = grafanaMetricsProperties;
    }

    @Override
    public StatsdReporter get() {
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsProperties);
        DataDogHandler dataDogHandler = new DataDogHandler(datadogMetricsProperties, metricsProperties);
       // GrafanaHandler grafanaHandler = new GrafanaHandler(grafanaMetricsProperties, metricsProperties);
        return new StatsdReporter(statsDFilter, statsDFormatter, dataDogHandler);

    }

}
