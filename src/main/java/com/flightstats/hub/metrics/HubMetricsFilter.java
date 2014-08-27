package com.flightstats.hub.metrics;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class HubMetricsFilter implements MetricFilter {

    @Override
    public boolean matches(String name, Metric metric) {
        if (name.contains(".test")) {
            return false;
        }
        if (Counting.class.isAssignableFrom(metric.getClass())) {
            return ((Counting) metric).getCount() > 0;
        }
        return true;
    }
}
