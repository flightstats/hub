package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Consumer;

public class StatsdReporter {

    private StatsDFilter statsDFilter;
    private StatsDFormatter statsDFormatter;
    private DataDogHandler dataDogHandler;

    public StatsdReporter(
            StatsDFilter statsDFilter,
            StatsDFormatter statsDFormatter,
            DataDogHandler dataDogHandler
    ) {
        this.statsDFilter = statsDFilter;
        this.statsDFormatter = statsDFormatter;
        this.dataDogHandler = dataDogHandler;
    }

    private void reportWithFilteredClients(String name, Consumer<StatsDClient> method) {
        List<StatsDClient> clients = statsDFilter.getFilteredClients(statsDFilter.isSecondaryReporting(name));
        clients.forEach(method);
    }

    private void reportWithBothClients(Consumer<StatsDClient> method) {
        List<StatsDClient> clients = statsDFilter.getFilteredClients(true);
        clients.forEach(method);
    }

    private void reportWithDefaultClient(Consumer<StatsDClient> method) {
        StatsDClient statsdClient = statsDFilter
                .getFilteredClients(false)
                .get(0);
        method.accept(statsdClient);
    }

    private void reportWithEitherClient(String metricTagName, Consumer<StatsDClient> method) {
        if (StringUtils.isNotBlank(metricTagName)) {
            reportWithFilteredClients(metricTagName, method);
        } else {
            reportWithDefaultClient(method);
        }
    }

    public void insert(String channel, long start, ChannelType type, int items, long bytes) {
        if (statsDFilter.isTestChannel(channel)) return;

        time(channel, "channel", start, bytes, "type:" + type.toString());
        count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
    }

    public void event(String title, String text, String[] tags) {
        Event event = statsDFormatter.buildCustomEvent(title, text);
        reportWithBothClients(statsDClient -> statsDClient.recordEvent(event, tags));
    }

    public void count(String name, long value, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.count(name, value, tags));
    }

    public void incrementCounter(String name, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.incrementCounter(name, tags));
    }

    public void increment(String name, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.increment(name, tags));
    }

    public void gauge(String name, double value, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.gauge(name, value, tags));
    }

    public void requestTime(long start, String ...tags) {
        reportWithBothClients((statsDClient) -> statsDClient.time("request", System.currentTimeMillis() - start, tags));
    }

    public void time(String name, long start, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.time(name, System.currentTimeMillis() - start, tags));
    }

    public void time(String channel, String name, long start, String... tags) {
        if (statsDFilter.isTestChannel(channel)) return;

        reportWithBothClients(statsDClient -> statsDClient.time(
                        name,
                        statsDFormatter.startTimeMillis(start),
                        statsDFormatter.formatChannelTags(channel, tags)
                ));
    }

    public void time(String channel, String name, long start, long bytes, String... tags) {
        if (statsDFilter.isTestChannel(channel)) return;

        time(channel, name, start, tags);
        count(name + ".bytes", bytes, statsDFormatter.formatChannelTags(channel, tags));

    }

    public void mute() {
        dataDogHandler.mute();
    }

}
