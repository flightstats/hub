package com.flightstats.hub.metrics;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class StatsDFilter {
    private final static String clientPrefix = "hub";
    private final static String clientHost = "localhost";
    private MetricsConfig metricsConfig;
    private StatsDClient statsDClient = new NoOpStatsDClient();
    private StatsDClient dataDogClient = new NoOpStatsDClient();

    @Inject
    public StatsDFilter(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    // initializing these clients starts their udp reporters, setting them explicitly in order to trigger them specifically
    void setOperatingClients() {
        int statsdPort = metricsConfig.getStatsdPort();
        int dogstatsdPort = metricsConfig.getDogstatsdPort();
        this.statsDClient = new NonBlockingStatsDClient(clientPrefix, clientHost, statsdPort);
        this.dataDogClient = new NonBlockingStatsDClient(clientPrefix, clientHost, dogstatsdPort);
    }

    boolean isSecondaryReporting(Webhook webhookConfig, ChannelConfig channelConfig) {
        Map<String, Boolean> valueMap = new HashMap<>();
        if (webhookConfig != null) {
            valueMap.put("webhook", webhookConfig.isSecondaryMetricsReporting());
        }
        if (channelConfig != null) {
            valueMap.put("channel", channelConfig.isSecondaryMetricsReporting());
        }
        return valueMap.values().contains(true);
    }

    List<StatsDClient> getFilteredClients(boolean secondaryReporting) {
        return secondaryReporting ?
                Arrays.asList(statsDClient, dataDogClient) :
                Collections.singletonList(statsDClient);
    }
}
