package com.flightstats.hub.metrics;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.inject.Inject;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;

import java.util.List;
import java.util.function.Consumer;

public class StatsdReporter {

    private StatsDFilter statsDFilter;
    private StatsDFormatter statsDFormatter;
    private DataDogHandler dataDogHandler;
    private ChannelService channelService;
    private WebhookService webhookService;

    @Inject
    public StatsdReporter(
            StatsDFilter statsDFilter,
            StatsDFormatter statsDFormatter,
            DataDogHandler dataDogHandler,
            ChannelService channelService,
            WebhookService webhookService
    ) {
        this.statsDFilter = statsDFilter;
        this.statsDFormatter = statsDFormatter;
        this.dataDogHandler = dataDogHandler;
        this.channelService = channelService;
        this.webhookService = webhookService;
    }

    private boolean isTestChannel(String channel) {
        return channel.toLowerCase().startsWith("test_");
    }

    private void reportWithFilteredClients(String name, Consumer<StatsDClient> method) {
        Webhook nullableWebhook = webhookService
                .get(name)
                .orElse(null);
        ChannelConfig nullableChannel = channelService
                .getCachedChannelConfig(name)
                .orElse(null);

        boolean isSecondaryReporting = statsDFilter.isSecondaryReporting(nullableWebhook, nullableChannel);
        List<StatsDClient> clients = statsDFilter.getFilteredClients(isSecondaryReporting);
        clients.forEach(method);
    }

    public void insert(String channel, long start, ChannelType type, int items, long bytes) {
        if (isTestChannel(channel)) return;

        time(channel, "channel", start, bytes, "type:" + type.toString());
        count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
    }

    public void event(String title, String text, String[] tags) {
        Event event = statsDFormatter.buildCustomEvent(title, text);
        reportWithFilteredClients(title, (statsDClient -> statsDClient.recordEvent(event, tags)));
    }

    public void count(String name, long value, String... tags) {
        reportWithFilteredClients(name, (statsDClient -> statsDClient.count(name, value, tags)));
    }

    public void incrementCounter(String name, String... tags) {
        reportWithFilteredClients(name, (statsDClient -> statsDClient.incrementCounter(name, tags)));
    }

    public void increment(String name, String... tags) {
        reportWithFilteredClients(name, (statsDClient) -> statsDClient.increment(name, tags));
    }

    public void gauge(String name, double value, String... tags) {
        reportWithFilteredClients(name, (statsDClient -> statsDClient.gauge(name, value, tags)));
    }

    public void time(String name, long start, String... tags) {
        reportWithFilteredClients(name, (statsDClient) -> statsDClient.time(name, System.currentTimeMillis() - start, tags));
    }

    public void time(String channel, String name, long start, String... tags) {
        if (isTestChannel(channel)) return;

        reportWithFilteredClients(channel, (
                statsDClient -> statsDClient.time(
                        name,
                        statsDFormatter.startTimeMillis(start),
                        statsDFormatter.formatChannelTags(channel, tags)
                )));
    }

    public void time(String channel, String name, long start, long bytes, String... tags) {
        if (isTestChannel(channel)) return;

        time(channel, name, start, tags);
        count(name + ".bytes", bytes, statsDFormatter.formatChannelTags(channel, tags));

    }

    public void mute() {
        if (dataDogHandler != null) {
            dataDogHandler.mute();
        }
    }

}
