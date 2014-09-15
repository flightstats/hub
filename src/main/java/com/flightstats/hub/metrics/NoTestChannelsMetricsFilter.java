package com.flightstats.hub.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class NoTestChannelsMetricsFilter implements MetricFilter {

    @Override
    public boolean matches(String name, Metric metric) {
        return !name.contains(".test");
    }
}
