package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.webhook.WebhookService;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class StatsDReporterProvider implements Provider<StatsdReporter> {
    private StatsDFilter statsDFilter;
    private MetricsConfig metricsConfig;
    private ChannelService channelService;
    private WebhookService webhookService;

    @Inject
    StatsDReporterProvider(
            StatsDFilter statsDFilter,
            MetricsConfig metricsConfig,
            ChannelService channelService,
            WebhookService webhookService
    ) {
        this.statsDFilter = statsDFilter;
        this.metricsConfig = metricsConfig;
        this.channelService = channelService;
        this.webhookService = webhookService;
    }

    @Override public StatsdReporter get() {
        StatsDFormatter statsDFormatter = new StatsDFormatter(metricsConfig);
        DataDogHandler dataDogHandler = new DataDogHandler(metricsConfig);
        return new StatsdReporter(statsDFilter, statsDFormatter, dataDogHandler, channelService, webhookService);
    }

}
