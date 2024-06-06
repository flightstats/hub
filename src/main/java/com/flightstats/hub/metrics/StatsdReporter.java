package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class StatsdReporter {

    private StatsDFilter statsDFilter;
    private StatsDFormatter statsDFormatter;
    private DataDogHandler dataDogHandler;

    private GrafanaHandler grafanaHandler;

    public StatsdReporter(
            StatsDFilter statsDFilter,
            StatsDFormatter statsDFormatter,
            DataDogHandler dataDogHandler,
            GrafanaHandler grafanaHandler
    ) {
        this.statsDFilter = statsDFilter;
        this.statsDFormatter = statsDFormatter;
        this.dataDogHandler = dataDogHandler;
        this.grafanaHandler = grafanaHandler;
    }

    public void insert(String channel, long start, ChannelMetricTag channelMetricTag, int items, long bytes) {
        if (statsDFilter.isTestChannel(channel)) return;

        time(channel, "channel", start, bytes, "type:" + channelMetricTag.name());
        count("channel.items", items, "type:" + channelMetricTag.name(), "channel:" + channel);
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

    public void incrementEventStart(MetricsType metricsType, String... additionalTags) {
        incrementCounter(metricsType.getEventType() + ".start", ArrayUtils.addAll(metricsType.getTags(), additionalTags));
    }

    public void incrementEventCompletion(MetricsType metricsType, String... additionalTags) {
        incrementCounter(metricsType.getEventType() + ".complete", ArrayUtils.addAll(metricsType.getTags(), additionalTags));
    }

    public void increment(String name, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.increment(name, tags));
    }

    public void gauge(String name, double value, String... tags) {
        reportWithBothClients(statsDClient -> statsDClient.gauge(name, value, tags));
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
        grafanaHandler.mute();
    }

    private void reportWithBothClients(Consumer<StatsDClient> method) {
        List<StatsDClient> clients = statsDFilter.getFilteredClients(true);
        clients.forEach(method);
    }

}
