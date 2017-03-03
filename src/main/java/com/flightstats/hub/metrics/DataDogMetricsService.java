package com.flightstats.hub.metrics;

import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class DataDogMetricsService implements MetricsService {
    private final static StatsDClient statsd = DataDog.statsd;

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {
        if (shouldLog(channel)) {
            time(channel, "channel", start, bytes, "type:" + type.toString());
            count("channel.items", items, "type:" + type.toString(), "channel:" + channel);
        }
    }

    @Override
    public void event(String title, String text, String[] tags) {
        Event event = DataDog.getEventBuilder()
                .withTitle(title)
                .withText(text)
                .build();
        DataDog.statsd.recordEvent(event, tags);
    }

    @Override
    public void count(String name, long value, String... tags) {
        statsd.count(name, value, tags);
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        statsd.gauge(name, value, tags);
    }

    @Override
    public void time(String name, long start, String... tags) {
        statsd.time(name, System.currentTimeMillis() - start, tags);
    }

    @Override
    public void time(String channel, String name, long start, String... tags) {
        if (shouldLog(channel)) {
            statsd.time(name, System.currentTimeMillis() - start, addChannelTag(channel, tags));
        }
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {
        if (shouldLog(channel)) {
            time(channel, name, start, tags);
            count(name + ".bytes", bytes, addChannelTag(channel, tags));
        }
    }

    String[] addChannelTag(String channel, String... tags) {
        List<String> tagList = Arrays.stream(tags).collect(Collectors.toList());
        tagList.add("channel:" + channel);
        return tagList.toArray(new String[tagList.size()]);
    }

}
