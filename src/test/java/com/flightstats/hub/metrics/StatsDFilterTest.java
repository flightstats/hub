package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatsDFilterTest {

    @Mock
    private DatadogMetricsProperties datadogMetricsProperties;
    @Mock
    private TickMetricsProperties tickMetricsProperties;
    @Mock
    private Dao<ChannelConfig> channelConfigDao;
    @Mock
    private Dao<Webhook> webhookDao;
    @Mock
    private ChannelConfig channelConfig;
    @Mock
    private Webhook webhook;
    private StatsDFilter statsDFilter;

    @BeforeEach
    void setup() {
        statsDFilter = new StatsDFilter(datadogMetricsProperties, tickMetricsProperties, channelConfigDao, webhookDao);
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseNoChannel() {
        when(channelConfigDao.getCached(anyString())).thenReturn(null);

        assertFalse(statsDFilter.shouldChannelReport("anyName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseNoWebhook() {
        when(webhookDao.getCached(anyString())).thenReturn(null);

        assertFalse(statsDFilter.shouldWebhookReport("anyName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseChannelNotReporting() {
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);

        assertFalse(statsDFilter.shouldChannelReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_trueChannelReporting() {
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);

        assertTrue(statsDFilter.shouldChannelReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_falseWebhookNotReporting() {
        when(webhook.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);

        assertFalse(statsDFilter.shouldWebhookReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterShouldChannelReport_trueWebhookReporting() {
        when(webhook.isSecondaryMetricsReporting()).thenReturn(true);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);

        assertTrue(statsDFilter.shouldWebhookReport("testName").isPresent());
    }

    @Test
    void testStatsDFilterGetAllClients_twoNoOpClients() {
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    void testStatsDFilterGetAllClients_twoCustomClients() {
        statsDFilter.setOperatingClients();
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getFilteredClients(true).get(1).getClass());
    }

    @Test
    void testStatsDFilterGetFilteredClients_oneClient() {
        assertEquals(1, statsDFilter.getFilteredClients(false).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(false).get(0).getClass());
    }

    @Test
    void testStatsDFilterGetFilteredClients_twoClientsFiltered() {
        assertEquals(2, statsDFilter.getFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getFilteredClients(true).get(0).getClass());
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_false() {
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);

        assertFalse(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_handlesNull() {
        when(channelConfigDao.getCached(anyString())).thenReturn(null);
        when(webhookDao.getCached(anyString())).thenReturn(null);

        assertFalse(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_trueIfAnyTrue() {
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        when(webhookDao.getCached(anyString())).thenReturn(webhook);
        when(webhook.isSecondaryMetricsReporting()).thenReturn(true);

        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_handleNullAndTrue() {
        when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);

        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterExtractName_handleNull() {
        assertEquals("", statsDFilter.extractName(null));
    }

    @Test
    void testStatsdFilterExtractName_handleNullTags() {
        String[] tags = {null, "tag2"};
        assertEquals("", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterExtractName_handleIrrelevantTags() {
        String[] tags = {"tag1", "tag2"};
        assertEquals("", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterExtractName_handleExtractChannelTags() {
        String[] tags = {"channel:test1", "tag2"};
        assertEquals("test1", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterExtractName_handleExtractWebhookTags() {
        String[] tags = {"name:test1", "tag2"};
        assertEquals("test1", statsDFilter.extractName(tags));
    }

    @Test
    void testStatsdFilterParseName_NullThrows() {
        Exception exception = assertThrows(NullPointerException.class, () -> statsDFilter.parseName.apply(null));
        assertEquals("java.lang.NullPointerException", exception.toString());
    }

    @Test
    void testStatsdFilterParseName_parsesName() {
        assertEquals("thisPart", statsDFilter.parseName.apply("notThisPart:thisPart"));
    }

    @Test
    void testStatsdFilterParseName_ignore() {
        assertEquals("", statsDFilter.parseName.apply("noPart"));
    }
}
