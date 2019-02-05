package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class StatsDBaseClient {
    private StatsDClient statsd;
    private MetricsConfig metricsConfig;
    private StatsDClientFilter StatsDClientFilter;

    public StatsDBaseClient(StatsDClient statsd, MetricsConfig metricsConfig) {
        this.statsd = statsd;
        this.metricsConfig =  metricsConfig;
    }

    private void conditionalReport(String channel, Consumer<String> callback) {
        if (!channel.toLowerCase().startsWith("test_")) {
            callback.accept(channel);
        }
    }

    public void insert(String channel, long start, MetricInsert type, int items, long bytes) {
        conditionalReport(
                channel,
                (String c) -> {
                    time(channel, "channel", start, bytes, "type:" + type.toString());
                    count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
                });
    }

    public void event(String title, String text, String[] tags) {
        Event event = Event.builder()
                .withHostname(metricsConfig.getHostTag())
                .withPriority(Event.Priority.NORMAL)
                .withAlertType(Event.AlertType.WARNING)
                .withTitle(title)
                .withText(text)
                .build();
        statsd.recordEvent(event, tags);
    }

    public void count(String name, long value, String... tags) {
        statsd.count(name, value, tags);
    }

    public void increment(String name, String... tags) {
        statsd.increment(name, tags);
    }

    public void gauge(String name, double value, String... tags) {
        statsd.gauge(name, value, tags);
    }

    public void time(String name, long start, String... tags) {
        statsd.time(name, System.currentTimeMillis() - start, tags);
    }

    public void time(String channel, String name, long start, String... tags) {
        conditionalReport(
                channel,
                (String c) -> statsd.time(name, System.currentTimeMillis() - start, addChannelTag(channel, tags)));
    }

    public void time(String channel, String name, long start, long bytes, String... tags) {
        conditionalReport(
                channel,
                (String c) -> {
                    time(channel, name, start, tags);
                    count(name + ".bytes", bytes, addChannelTag(channel, tags));
                });
    }

    private String[] addChannelTag(String channel, String... tags) {
        List<String> tagList = Arrays
                .stream(tags)
                .collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[0]);
    }

    public void mute() {
        //
    }

}
