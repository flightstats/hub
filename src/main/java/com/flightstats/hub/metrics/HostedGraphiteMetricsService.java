package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProvider;

public class HostedGraphiteMetricsService implements MetricsService {

    private final static MetricsSender sender = HubProvider.getInstance(MetricsSender.class);

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {
        sender.send("channel." + channel + ".post", start);
        sender.send("channel." + channel + ".items", items);
        sender.send("channel." + channel + ".post.bytes", bytes);
        sender.send("channel.ALL.post", System.currentTimeMillis() - start);
    }

    @Override
    public void event(String title, String text, String[] tags) {
        //do nothing
    }

    @Override
    public void count(String name, long value, String... tags) {
        sender.send(name, value);
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        sender.send(name, value);
    }

    @Override
    public void time(String channel, String name, long start, String... tags) {
        sender.send("channel." + channel + "." + name, 1);
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {
        sender.send("channel." + channel + "." + name, 1);
        sender.send("channel." + channel + "." + name + ".bytes", bytes);
    }
}
