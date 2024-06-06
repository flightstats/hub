package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.GrafanaMetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.RequestMetric;
import com.flightstats.hub.webhook.Webhook;

import javax.inject.Inject;

import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@Singleton
public class StatsDFilter {
    private final static String clientPrefix = "hub";
    private final static String clientHost = "localhost";
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();
    private StatsDClient dataGrafanaClient = new NoOpStatsDClient();


    private final DatadogMetricsProperties datadogMetricsProperties;

    private final GrafanaMetricsProperties grafanaMetricsProperties;

    private final TickMetricsProperties tickMetricsProperties;
    // going direct to the DAO here over channelService/webhookService to avoid circular dep. condition in Guice injections
    private final Dao<ChannelConfig> channelConfigDao;
    private final Dao<Webhook> webhookConfigDao;

    private final Set<String> requestMetricsToIgnore;

    private final Set<String> requestMetricsToIgnoreGrafana;

    @Inject
    public StatsDFilter(
            DatadogMetricsProperties datadogMetricsProperties,
            TickMetricsProperties tickMetricsProperties,
            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
            @Named("Webhook") Dao<Webhook> webhookConfigDao, GrafanaMetricsProperties grafanaMetricsProperties) {
        this.datadogMetricsProperties = datadogMetricsProperties;
        this.tickMetricsProperties = tickMetricsProperties;
        this.channelConfigDao = channelConfigDao;
        this.webhookConfigDao = webhookConfigDao;
        this.grafanaMetricsProperties = grafanaMetricsProperties;

        String[] ignoredRequestMetrics = StringUtils.split(datadogMetricsProperties.getRequestMetricsToIgnore());
        requestMetricsToIgnore = (null != ignoredRequestMetrics && ignoredRequestMetrics.length > 0)
                ? new HashSet<>(Arrays.asList(ignoredRequestMetrics))
                : Collections.emptySet();

        String[] ignoredGrafanaRequestMetrics = StringUtils.split(grafanaMetricsProperties.getRequestMetricsToIgnore());
        requestMetricsToIgnoreGrafana = (null != ignoredGrafanaRequestMetrics && ignoredGrafanaRequestMetrics.length > 0)
                ? new HashSet<>(Arrays.asList(ignoredGrafanaRequestMetrics))
                : Collections.emptySet();
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        int statsdPort = tickMetricsProperties.getStatsdPort();
        int dogstatsdPort = datadogMetricsProperties.getStatsdPort();
        int grafanaStatsdPort = grafanaMetricsProperties.getStatsdPort();
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, statsdPort);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, dogstatsdPort);
        this.dataGrafanaClient = new NonBlockingStatsDClient(clientPrefix, clientHost, grafanaStatsdPort);
    }

    public boolean isTestChannel(String channel) {
        return channel.toLowerCase().startsWith("test_");
    }

    public boolean isIgnoredRequestMetric(RequestMetric metric) {
        return metric.getMetricName()
                .map(requestMetricsToIgnore::contains)
                .orElse(true);
    }

    List<StatsDClient> getFilteredClients(boolean secondaryReporting) {
        StatsDClient primaryClient = datadogMetricsProperties.isPrimary() ? dataDogClient : statsDClient;
        StatsDClient secondaryClient = primaryClient.equals(dataDogClient) ? statsDClient : dataDogClient;

        StatsDClient primaryGrafanaClient = grafanaMetricsProperties.isPrimary() ? dataGrafanaClient : statsDClient;
        StatsDClient secondaryGrafanaClient = primaryGrafanaClient.equals(dataGrafanaClient) ? statsDClient : dataGrafanaClient;

        return secondaryReporting ?
                Arrays.asList(primaryClient, secondaryClient) :
                Collections.singletonList(primaryClient);
    }

    List<StatsDClient> getGrafanaFilteredClients(boolean reporting) {
        StatsDClient primaryGrafanaClient = grafanaMetricsProperties.isPrimary() ? dataGrafanaClient : statsDClient;
        StatsDClient secondaryGrafanaClient = primaryGrafanaClient.equals(dataGrafanaClient) ? statsDClient : dataGrafanaClient;

        return reporting ?
                Arrays.asList(primaryGrafanaClient, secondaryGrafanaClient) :
                Collections.singletonList(primaryGrafanaClient);
    }


    boolean isSecondaryReporting(String name) {
        return shouldChannelReport(name).isPresent() || shouldWebhookReport(name).isPresent();
    }

    Optional<ChannelConfig> shouldChannelReport(String name) {
        return Optional
                .ofNullable(channelConfigDao.getCached(name))
                .filter(ChannelConfig::isSecondaryMetricsReporting);
    }

    Optional<Webhook> shouldWebhookReport(String name) {
        return Optional
                .ofNullable(webhookConfigDao.getCached(name))
                .filter(Webhook::isSecondaryMetricsReporting);
    }

    String extractName(String[] tags) {
        // webhook tags may contain name:webhookName
        // channel tags may contain channel:channelName
        try {
            return Stream.of(tags)
                    .filter(StringUtils::isNotBlank)
                    .filter(str -> str.contains("name") || str.contains("channel"))
                    .map(parseName)
                    .findAny()
                    .orElse("");
        } catch (Exception ex) {
            return "";
        }
    }

    Function<String, String> parseName = (String str) -> str.contains(":") ? str.substring(str.indexOf(":") + 1) : "";
}
