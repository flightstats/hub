package com.flightstats.hub.metrics;

public class NoOpMetricsSender implements MetricsSender {
    @Override
    public void send(String name, Object value) {
        //do nothing
    }
}
