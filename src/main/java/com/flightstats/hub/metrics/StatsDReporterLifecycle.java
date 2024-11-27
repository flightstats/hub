package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.MetricsProperties;
import com.google.common.util.concurrent.AbstractIdleService;
import javax.inject.Inject;
import com.timgroup.statsd.StatsDClient;

public class StatsDReporterLifecycle extends AbstractIdleService {
    private final StatsDFilter statsDFilter;
    private final MetricsProperties metricsProperties;

    @Inject
    public StatsDReporterLifecycle(StatsDFilter statsDFilter,
                                   MetricsProperties metricsProperties) {
        this.statsDFilter = statsDFilter;
        this.metricsProperties = metricsProperties;
    }

    public void startUp() {
        if (metricsProperties.isEnabled()) {
            statsDFilter.setOperatingClients();
        }
    }

    public void shutDown() {
        statsDFilter.getGrafanaFilteredClients(true)
                .forEach(StatsDClient::stop);
    }
}
