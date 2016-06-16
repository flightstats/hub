package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.timgroup.statsd.ServiceCheck;
import com.timgroup.statsd.StatsDClient;

public class NoOpStatsD implements StatsDClient {

    @Inject
    public NoOpStatsD() {

    }

    public void stop() {
    }

    public void count(String aspect, long delta, String... tags) {
    }

    public void incrementCounter(String aspect, String... tags) {
    }

    public void increment(String aspect, String... tags) {
    }

    public void decrement(String aspect, String... tags) {
    }

    public void decrementCounter(String aspect, String... tags) {
    }

    public void recordGaugeValue(String aspect, double value, String... tags) {
    }

    public void gauge(String aspect, double value, String... tags) {
    }

    public void recordGaugeValue(String aspect, long value, String... tags) {
    }

    public void gauge(String aspect, long value, String... tags) {
    }

    public void recordExecutionTime(String aspect, long value, String... tags) {
    }

    public void time(String aspect, long value, String... tags) {
    }

    public void recordHistogramValue(String aspect, double value, String... tags) {
    }

    public void histogram(String aspect, double value, String... tags) {
    }

    public void recordHistogramValue(String aspect, long value, String... tags) {
    }

    public void histogram(String aspect, long value, String... tags) {
    }

    public void recordServiceCheckRun(ServiceCheck sc) {
    }

    public void serviceCheck(ServiceCheck sc) {
    }

}
