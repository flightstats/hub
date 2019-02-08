package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
public class HostedGraphiteMetricsService implements MetricsService {

    @Inject
    private HostedGraphiteSender sender;

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
    public void increment(String name, String... tags) {
        throw new NotImplementedException("'increment' not implemented for hosted Graphite");
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        sender.send(name, value);
    }

    @Override
    public void time(String name, long start, String... tags) {
        sender.send(name, System.currentTimeMillis() - start);
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

    @Override
    public void mute() {};
}
