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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        if (name.equals("request")) return false;

        return Optional
                .ofNullable(channelConfigDao.getCached(name))
                .filter(ChannelConfig::isSecondaryMetricsReporting)
                .isPresent() ||
                Optional
                .ofNullable(webhookConfigDao.getCached(name))
                .filter(Webhook::isSecondaryMetricsReporting)
                .isPresent();
    }

    List<StatsDClient> getFilteredClients(boolean secondaryReporting) {
        return secondaryReporting ?
                Arrays.asList(statsDClient, dataDogClient) :
                Collections.singletonList(statsDClient);
    }
}
