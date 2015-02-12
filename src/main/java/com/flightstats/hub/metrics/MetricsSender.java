package com.flightstats.hub.metrics;

public interface MetricsSender {
    void send(String name, Object value);
}
