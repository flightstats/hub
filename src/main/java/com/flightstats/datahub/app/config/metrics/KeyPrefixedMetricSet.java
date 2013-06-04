package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.util.HashMap;
import java.util.Map;

class KeyPrefixedMetricSet implements MetricSet {

	private final MetricSet delegate;
	private final String prefix;

	KeyPrefixedMetricSet(String prefix, MetricSet delegate) {
		this.delegate = delegate;
		this.prefix = prefix;
	}

	static KeyPrefixedMetricSet prefix(String prefix, MetricSet delegate) {
		return new KeyPrefixedMetricSet(prefix, delegate);
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> result = new HashMap<>();
		for (Map.Entry<String, Metric> entry : delegate.getMetrics().entrySet()) {
			String metricName = entry.getKey();
			Metric metric = entry.getValue();
			result.put(prefix + "." + metricName, metric);
		}
		return result;
	}
}
