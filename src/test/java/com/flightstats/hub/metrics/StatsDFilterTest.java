package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.GrafanaMetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.RequestMetric;
import com.flightstats.hub.webhook.Webhook;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class StatsDFilterTest {

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
    @Mock
    private GrafanaMetricsProperties grafanaMetricsProperties;

    @BeforeEach
    void setup() {
        statsDFilter = new StatsDFilter(tickMetricsProperties, channelConfigDao, webhookDao, grafanaMetricsProperties);
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
        assertEquals(2, statsDFilter.getGrafanaFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getGrafanaFilteredClients(true).get(0).getClass());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getGrafanaFilteredClients(true).get(1).getClass());
    }

    @Test
    void testStatsDFilterGetAllClients_twoCustomClients() {
        statsDFilter.setOperatingClients();
        assertEquals(2, statsDFilter.getGrafanaFilteredClients(true).size());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getGrafanaFilteredClients(true).get(0).getClass());
        assertEquals(NonBlockingStatsDClient.class, statsDFilter.getGrafanaFilteredClients(true).get(1).getClass());
    }

    @Test
    void testStatsDFilterGetFilteredClients_oneClient() {
        assertEquals(1, statsDFilter.getGrafanaFilteredClients(false).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getGrafanaFilteredClients(false).get(0).getClass());
    }

    @Test
    void testStatsDFilterGetFilteredClients_twoClientsFiltered() {
        assertEquals(2, statsDFilter.getGrafanaFilteredClients(true).size());
        assertEquals(NoOpStatsDClient.class, statsDFilter.getGrafanaFilteredClients(true).get(0).getClass());
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
        Mockito.lenient().when(channelConfigDao.getCached(anyString())).thenReturn(null);
        Mockito.lenient().when(webhookDao.getCached(anyString())).thenReturn(null);

        assertFalse(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_trueIfWebhookIsTrue() {
        Mockito.lenient().when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        Mockito.lenient().when(channelConfig.isSecondaryMetricsReporting()).thenReturn(false);
        Mockito.lenient().when(webhookDao.getCached(anyString())).thenReturn(webhook);
        Mockito.lenient().when(webhook.isSecondaryMetricsReporting()).thenReturn(true);

        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_trueIfChannelIsTrue() {
        Mockito.lenient().when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        Mockito.lenient().when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);
        Mockito.lenient().when(webhookDao.getCached(anyString())).thenReturn(webhook);
        Mockito.lenient().when(webhook.isSecondaryMetricsReporting()).thenReturn(false);

        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_handleNullWebhookAndTrueChannel() {
        Mockito.lenient().when(webhookDao.getCached(anyString())).thenReturn(null);
        Mockito.lenient().when(channelConfigDao.getCached(anyString())).thenReturn(channelConfig);
        Mockito.lenient().when(channelConfig.isSecondaryMetricsReporting()).thenReturn(true);

        assertTrue(statsDFilter.isSecondaryReporting("foo"));
    }

    @Test
    void testStatsdFilterIsSecondaryReporting_handleNullChannelAndTrueWebhook() {
        Mockito.lenient().when(channelConfigDao.getCached(anyString())).thenReturn(null);
        Mockito.lenient().when(webhookDao.getCached(anyString())).thenReturn(webhook);
        Mockito.lenient().when(webhook.isSecondaryMetricsReporting()).thenReturn(true);

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

    @Test
    void testIsIgnoredRequestMetric_ignored() {
        RequestMetric metric = mock(RequestMetric.class);
        when(metric.getMetricName()).thenReturn(Optional.of("request.api.channel"));

        when(grafanaMetricsProperties.getRequestMetricsToIgnore()).thenReturn("request.api.something request.api.channel");
        statsDFilter = new StatsDFilter(tickMetricsProperties, channelConfigDao, webhookDao, grafanaMetricsProperties);

        assertTrue(statsDFilter.isIgnoredGrRequestMetric(metric));
    }

    @Test
    void testIsIgnoredRequestMetric_notIgnored() {
        RequestMetric metric = mock(RequestMetric.class);
        when(metric.getMetricName()).thenReturn(Optional.of("request.internal.channel"));

        when(grafanaMetricsProperties.getRequestMetricsToIgnore()).thenReturn("request.api.something request.api.channel");
        statsDFilter = new StatsDFilter(tickMetricsProperties, channelConfigDao, webhookDao, grafanaMetricsProperties);

        assertFalse(statsDFilter.isIgnoredGrRequestMetric(metric));
    }

    @Test
    void testIgnoreRequestMetric_ignoreIfMetricsNameIsBlankForSomeReason() {
        RequestMetric metric = mock(RequestMetric.class);
        when(metric.getMetricName()).thenReturn(Optional.empty());

        when(grafanaMetricsProperties.getRequestMetricsToIgnore()).thenReturn("request.api.something request.api.channel");
        statsDFilter = new StatsDFilter(tickMetricsProperties, channelConfigDao, webhookDao, grafanaMetricsProperties);

        assertTrue(statsDFilter.isIgnoredGrRequestMetric(metric));
    }
}
