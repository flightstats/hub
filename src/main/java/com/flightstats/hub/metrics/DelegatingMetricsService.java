package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class DelegatingMetricsService implements MetricsService {

    private List<MetricsService> services = new ArrayList<>();

    public DelegatingMetricsService() {
        if (HubProperties.getProperty("hosted_graphite.enable", false)) {
            services.add(new HostedGraphiteMetricsService());
        }
        if (HubProperties.getProperty("data_dog.enable", false)) {
            services.add(new DataDogMetricsService());
        }
    }

    @Override
    public void insert(String channel, long start, Insert type, int items, long bytes) {
        services.forEach((service) -> service.insert(channel, start, type, items, bytes));
    }

    @Override
    public void event(String title, String text, String... tags) {
        services.forEach((service) -> service.event(title, text, tags));
    }

    @Override
    public void count(String name, long value, String... tags) {
        services.forEach((service) -> service.count(name, value, tags));
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        services.forEach((service) -> service.gauge(name, value, tags));
    }

    @Override
    public void time(String name, long start, String... tags) {
        services.forEach((service) -> service.time(name, start, tags));
    }

    @Override
    public void time(String channel, String name, long start, String... tags) {
        services.forEach((service) -> service.time(channel, name, start, tags));
    }

    @Override
    public void time(String channel, String name, long start, long bytes, String... tags) {
        services.forEach((service) -> service.time(channel, name, start, bytes, tags));
    }
}
