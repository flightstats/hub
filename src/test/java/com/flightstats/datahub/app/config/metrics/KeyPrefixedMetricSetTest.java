package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeyPrefixedMetricSetTest {

	@Test
	public void testPrefixedKeys() throws Exception {
		//GIVEN
		MetricSet set = mock(MetricSet.class);
		Map<String, Metric> metricsMap = new HashMap<>();
		Metric mock1 = mock(Metric.class);
		Metric mock2 = mock(Metric.class);
		Metric mock3 = mock(Metric.class);
		Metric mock4 = mock(Metric.class);
		metricsMap.put("foo", mock1);
		metricsMap.put("bar", mock2);
		metricsMap.put("baz", mock3);
		metricsMap.put("bongo", mock4);
		when(set.getMetrics()).thenReturn(metricsMap);
		KeyPrefixedMetricSet testClass = new KeyPrefixedMetricSet("my.custom", set);

		//WHEN
		Map<String, Metric> result = testClass.getMetrics();

		//THEN
		assertEquals(result.get("my.custom.foo"), mock1);
		assertEquals(result.get("my.custom.bar"), mock2);
		assertEquals(result.get("my.custom.baz"), mock3);
		assertEquals(result.get("my.custom.bongo"), mock4);
	}
}
