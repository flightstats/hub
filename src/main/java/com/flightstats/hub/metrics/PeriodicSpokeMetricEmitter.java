package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.spoke.SpokeStore;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PeriodicSpokeMetricEmitter implements PeriodicMetricEmitter {
    private StatsdReporter statsdReporter;

    @Inject
    PeriodicSpokeMetricEmitter(StatsdReporter statsdReporter) {
        this.statsdReporter = statsdReporter;
    }

    @Override
    public void emit() {
        statsdReporter.gauge("spoke.write.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
        statsdReporter.gauge("spoke.read.ttl", HubProperties.getSpokeTtlMinutes(SpokeStore.READ));
    }
}
