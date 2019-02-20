package com.flightstats.hub.metrics;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void testStatsDFilterIsSecondaryReporting_TwoNullsFalse() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertFalse("expected true to be false", statsDFilter.isSecondaryReporting(null, null));
    }

    @Test
    public void testStatsDFilterIsSecondaryReporting_webhookTrue() {
        Webhook webhook = mock(Webhook.class);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(true);
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertTrue("expected false to be true", statsDFilter.isSecondaryReporting(webhook, null));
    }

    @Test
    public void testStatsDFilterIsSecondaryReporting_channelTrue() {
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertTrue("expected false to be true", statsDFilter.isSecondaryReporting(null, channelConfig));
    }

    @Test
    public void testStatsDFilterIsSecondaryReporting_bothTrue() {
        Webhook webhook = mock(Webhook.class);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(true);
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertTrue("expected false to be true", statsDFilter.isSecondaryReporting(webhook, channelConfig));
    }

    @Test
    public void testStatsDFilterIsSecondaryReporting_bothFalse() {
        Webhook webhook = mock(Webhook.class);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(false);
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertFalse("expected true to be false", statsDFilter.isSecondaryReporting(webhook, channelConfig));
    }

    @Test
    public void testStatsDFilterIsSecondaryReporting_anyTrueForTrue() {
        boolean trueOrFalse = new Random().nextBoolean();
        boolean opposite = !trueOrFalse;
        Webhook webhook = mock(Webhook.class);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(trueOrFalse);
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(opposite);
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig);
        assertTrue("expected false to be true", statsDFilter.isSecondaryReporting(webhook, channelConfig));
    }
}
