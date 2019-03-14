package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Singleton
public class StatsDFilter {
    private final static String clientPrefix = "hub";
    private final static String clientHost = "localhost";
    private MetricsConfig metricsConfig;
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();
    // going direct to the DAO here over channelService/webhookService to avoid circular dep. condition in Guice injections
    private Dao<ChannelConfig> channelConfigDao;
    private Dao<Webhook> webhookConfigDao;

    @Inject
    public StatsDFilter(
            MetricsConfig metricsConfig,
            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
            @Named("Webhook") Dao<Webhook> webhookConfigDao
    ) {
        this.metricsConfig = metricsConfig;
        this.channelConfigDao = channelConfigDao;
        this.webhookConfigDao = webhookConfigDao;
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        int statsdPort = metricsConfig.getStatsdPort();
        int dogstatsdPort = metricsConfig.getDogstatsdPort();
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, statsdPort);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, dogstatsdPort);
    }

    public boolean isTestChannel(String channel) {
        return channel.toLowerCase().startsWith("test_");
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

    List<StatsDClient> getFilteredClients(boolean secondaryReporting) {
        return secondaryReporting ?
                Arrays.asList(statsDClient, dataDogClient) :
                Collections.singletonList(statsDClient);
    }
}
