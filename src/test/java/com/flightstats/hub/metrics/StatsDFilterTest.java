package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.DynamoChannelConfigDao;
import com.flightstats.hub.dao.aws.DynamoWebhookDao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsDFilterTest {

    private Dao<ChannelConfig> getMockedChannelConfigDao() {
        return mock(DynamoChannelConfigDao.class);
    }

    private Dao<Webhook> getMockedWebhookConfigDao() {
        return mock(DynamoWebhookDao.class);
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseNoChannel() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(null);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.shouldChannelReport("anyName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseNoWebhook() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(null);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.shouldWebhookReport("anyName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseChannelNotReporting() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.shouldChannelReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_trueChannelReporting() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertTrue(statsDFilter.shouldChannelReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseWebhookNotReporting() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        Webhook webhook = mock(Webhook.class);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);
        when(channelConfigDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.shouldWebhookReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_trueWebhookReporting() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        Webhook webhook = mock(Webhook.class);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(true);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);
        when(channelConfigDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertTrue(statsDFilter.shouldWebhookReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterGetAllClients_twoNoOpClients() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    void testStatsDFilterGetAllClients_twoCustomClients() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        statsDFilter.setOperatingClients();
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    void testStatsDFilterGetFilteredClients_oneClient() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals(1, statsDFilter.getFilteredClients(false).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(false).get(0).getClass());
    }

    @Test
    void testStatsDFilterGetFilteredClients_twoClientsFiltered() {
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_false() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        ChannelConfig channelConfig = mock(ChannelConfig.class);
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();
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
    void testStatsdFilterIsSecondaryReporting_handlesNull() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();
        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(null);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertFalse(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_trueIfAnyTrue() {
        // GIVEN
        boolean trueOrFalse = new Random().nextBoolean();
        boolean opposite = !trueOrFalse;
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        ChannelConfig channelConfig = mock(ChannelConfig.class);

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();
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
    void testStatsdFilterIsSecondaryReporting_handleNullAndTrue() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();
        ChannelConfig channelConfig = mock(ChannelConfig.class);

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // WHEN
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterExtractName_handleNull() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("", statsDFilter.extractName(null));
    }

    @Test
    void testStatsdFilterExtractName_handleNullTags() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        String[] tags = { null, "tag2" };
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterExtractName_handleIrrelevantTags() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        String[] tags = { "tag1", "tag2" };
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterExtractName_handleExtractChannelTags() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        String[] tags = { "channel:test1", "tag2" };
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("test1", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterExtractName_handleExtractWebhookTags() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        String[] tags = { "name:test1", "tag2" };
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("test1", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterParseName_NullThrows() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        Exception exception = assertThrows(NullPointerException.class, () -> statsDFilter.parseName.apply(null));
        assertEquals("java.lang.NullPointerException", exception.toString());
    }

    @Test
    void testStatsdFilterParseName_parsesName() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("thisPart", statsDFilter.parseName.apply("notThisPart:thisPart"));
    }

    @Test
    void testStatsdFilterParseName_ignore() {
        // GIVEN
        MetricsConfig metricsConfig = MetricsConfig.builder().build();

        Dao<ChannelConfig> channelConfigDao = getMockedChannelConfigDao();

        Dao<Webhook> webhookDao =  getMockedWebhookConfigDao();

        // THEN
        StatsDFilter statsDFilter = new StatsDFilter(metricsConfig, channelConfigDao, webhookDao);
        assertEquals("", statsDFilter.parseName.apply("noPart"));
    }
}
