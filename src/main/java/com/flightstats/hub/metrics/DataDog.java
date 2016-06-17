package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

@Singleton
public class DataDog {
    public final static StatsDClient statsd = HubProperties.getProperty("data_dog.enable", false) ?
            new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"})
            : new NoOpStatsD();

    @Inject
    public DataDog() {
    }

}

