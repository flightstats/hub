package com.flightstats.hub.metrics;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.Test;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class StatsDFilterTest {

    @Test
    public void testStatsDFilterGetAllClients_twoNoOpClients() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    public void testStatsDFilterGetAllClients_twoCustomClients() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        statsDFilter.setOperatingClients();
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    public void testStatsDFilterGetFilteredClients_oneClient() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertEquals(1, statsDFilter.getFilteredClients(false).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(false).get(0).getClass());
    }

    @Test
    public void testStatsDFilterGetFilteredClients_twoClientsFiltered() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
    }
}
