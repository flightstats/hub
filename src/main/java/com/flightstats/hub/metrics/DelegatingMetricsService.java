package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class DelegatingMetricsService implements MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(DelegatingMetricsService.class);

    private List<MetricsService> services = new ArrayList<>();

    public DelegatingMetricsService() {
        HubServices.register(new DelegatingMetricsServiceInitial(), HubServices.TYPE.BEFORE_HEALTH_CHECK);
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
    public void increment(String name, String... tags) {
        services.forEach((service) -> service.increment(name, tags));
    }

    @Override
    public void gauge(String name, double value, String... tags) {
        services.forEach((service) -> service.gauge(name, value, tags));
    }

    @Override
    public void mute() {
        services.forEach((service) -> service.mute());
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

    private class DelegatingMetricsServiceInitial extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            List<MetricsService> newServices = new ArrayList<>();
            if (HubProperties.getProperty("hosted_graphite.enable", false)) {
                logger.info("starting hosted graphite");
                newServices.add(HubProvider.getInstance(HostedGraphiteMetricsService.class));
                logger.info("started hosted graphite");
            }
            if (HubProperties.getProperty("metrics.enable", false)) {
                logger.info("starting datadog");
                newServices.add(HubProvider.getInstance(DataDogMetricsService.class));
                logger.info("started datadog");
            }
            services = newServices;
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }
}
