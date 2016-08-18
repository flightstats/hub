package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Singleton;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

@Singleton
public class DataDog {
    public final static StatsDClient statsd = HubProperties.getProperty("data_dog.enable", false) ?
            new NonBlockingStatsDClient("hub", "localhost", 8125, "tag:value")
            : new NoOpStatsDClient();

}

