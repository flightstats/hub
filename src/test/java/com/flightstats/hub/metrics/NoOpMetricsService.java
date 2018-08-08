package com.flightstats.hub.metrics;

public class NoOpMetricsService implements MetricsService {

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {

    }

    @Override
    public void event(String title, String text, String[] tags) {

    }

    @Override
    public void count(String name, long value, String... tags) {

    }

    @Override
    public void increment(String name, String... tags) {

    }

    @Override
    public void gauge(String name, double value, String... tags) {

    }

    @Override
    public void time(String name, long start, String... tags) {

    }

    @Override
    public void time(String channel, String name, long start, String... tags) {

    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {

    }

    @Override
    public void mute() {};
}
