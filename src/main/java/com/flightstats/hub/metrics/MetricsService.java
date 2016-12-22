package com.flightstats.hub.metrics;

public interface MetricsService {

    enum Insert {
        single,
        historical,
        bulk
    }

    void insert(String channel, long start, Insert type, int items, long bytes);

    void event(String title, String text, String... tags);

    void count(String name, long value, String... tags);

    void gauge(String name, double value, String... tags);

    void time(String channel, String name, long start, String... tags);

    void time(String channel, String name, long start, long bytes, String... tags);
}
