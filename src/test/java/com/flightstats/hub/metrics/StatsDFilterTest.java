package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatsDFilterTest {

    @Test
    @SuppressWarnings("unchecked")
    // suppressing the unchecked warning here, the cast to Dao<T> is safe in this case
    public void testStatsDFilterGetAllClients_twoNoOpClients() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsDFilterGetAllClients_twoCustomClients() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        statsDFilter.setOperatingClients();
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsDFilterGetFilteredClients_oneClient() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals(1, statsDFilter.getFilteredClients(false).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(false).get(0).getClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsDFilterGetFilteredClients_twoClientsFiltered() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsdFilterIsSecondaryReporting_false() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);
        Webhook webhook = mock(Webhook.class);

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsdFilterIsSecondaryReporting_handlesNull() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        Dao<Webhook> webhookDao =  (Dao<Webhook>) mock(CachedDao.class);

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(null);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsdFilterIsSecondaryReporting_trueIfAnyTrue() {
        // GIVEN
        boolean trueOrFalse = new Random().nextBoolean();
        boolean opposite = !trueOrFalse;
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        ChannelConfig channelConfig = mock(ChannelConfig.class);

        Dao<Webhook> webhookDao = (Dao<Webhook>) mock(CachedDao.class);
        Webhook webhook = mock(Webhook.class);

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(trueOrFalse);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(opposite);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStatsdFilterIsSecondaryReporting_handleNullAndTrue() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = (Dao<ChannelConfig>) mock(CachedLowerCaseDao.class);
        ChannelConfig channelConfig = mock(ChannelConfig.class);

        Dao<Webhook> webhookDao = (Dao<Webhook>) mock(CachedDao.class);

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }
}
