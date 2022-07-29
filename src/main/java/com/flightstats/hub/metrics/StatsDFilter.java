package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.TickMetricsProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
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

    private final DatadogMetricsProperties datadogMetricsProperties;
    private final TickMetricsProperties tickMetricsProperties;
    // going direct to the DAO here over channelService/webhookService to avoid circular dep. condition in Guice injections
    private final Dao<ChannelConfig> channelConfigDao;
    private final Dao<Webhook> webhookConfigDao;

    private final Set<String> requestMetricsToIgnore;

    @Inject
    public StatsDFilter(
            DatadogMetricsProperties datadogMetricsProperties,
            TickMetricsProperties tickMetricsProperties,
            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
            @Named("Webhook") Dao<Webhook> webhookConfigDao) {
        this.datadogMetricsProperties = datadogMetricsProperties;
        this.tickMetricsProperties = tickMetricsProperties;
        this.channelConfigDao = channelConfigDao;
        this.webhookConfigDao = webhookConfigDao;

        String[] ignoredRequestMetrics = StringUtils.split(datadogMetricsProperties.getRequestMetricsToIgnore());
        requestMetricsToIgnore = (null != ignoredRequestMetrics && ignoredRequestMetrics.length > 0)
                ? new HashSet<>(Arrays.asList(ignoredRequestMetrics))
                : Collections.emptySet();
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        int statsdPort = tickMetricsProperties.getStatsdPort();
        int dogstatsdPort = datadogMetricsProperties.getStatsdPort();
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, statsdPort);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, dogstatsdPort);
    }

    public boolean isTestChannel(String channel) {
        return channel.toLowerCase().startsWith("test_");
    }

    public boolean isIgnoredRequestMetric(String metric) {
        return requestMetricsToIgnore.contains(metric);
    }

    List<StatsDClient> getFilteredClients(boolean secondaryReporting) {
        StatsDClient primaryClient = datadogMetricsProperties.isPrimary() ? dataDogClient : statsDClient;
        StatsDClient secondaryClient = primaryClient.equals(dataDogClient) ? statsDClient : dataDogClient;
        return secondaryReporting ?
                Arrays.asList(primaryClient, secondaryClient) :
                Collections.singletonList(primaryClient);
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
