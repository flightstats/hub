package com.flightstats.hub.metrics;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.timgroup.statsd.StatsDClient;

public class StatsDReporterLifecycle extends AbstractIdleService {
    private StatsDFilter statsDFilter;

    @Inject
    public StatsDReporterLifecycle(StatsDFilter statsDFilter) {
        this.statsDFilter = statsDFilter;
    }

    public void startUp() {
        statsDFilter.setOperatingClients();
    }

    public void shutDown() {
        statsDFilter.getAllClients().forEach(StatsDClient::stop);
    }
}
