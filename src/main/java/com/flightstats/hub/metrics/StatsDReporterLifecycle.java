package com.flightstats.hub.metrics;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.timgroup.statsd.StatsDClient;

public class StatsDReporterLifecycle extends AbstractIdleService {
    private StatsDFilter statsDFilter;
    private MetricsConfig metricsConfig;

    @Inject
    public StatsDReporterLifecycle(StatsDFilter statsDFilter,
                                   MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
        this.statsDFilter = statsDFilter;
    }

    public void startUp() {
        if (metricsConfig.isEnabled()) {
            statsDFilter.setOperatingClients();
        }
    }

    public void shutDown() {
        statsDFilter.getAllClients().forEach(StatsDClient::stop);
    }
}
