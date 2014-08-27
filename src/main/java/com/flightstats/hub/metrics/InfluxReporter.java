package com.flightstats.hub.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.influxdb.dto.Serie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class InfluxReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger(InfluxReporter.class);

    private final InfluxConfig config;

    public InfluxReporter(InfluxConfig config) {
        super(config.getRegistry(), "influxdb-reporter", config.getFilter(), config.getRateUnit(), config.getDurationUnit());
        this.config = config;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        long now = config.getClock().getTime();
        List<Serie> series = new ArrayList<>();
        try {
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                series.add(gauge(entry.getKey(), entry.getValue(), now));
            }
            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                series.add(counter(entry.getKey(), entry.getValue(), now));
            }
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                series.add(histogram(entry.getKey(), entry.getValue(), now));
            }
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                series.add(meter(entry.getKey(), entry.getValue(), now));
            }
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                series.add(timer(entry.getKey(), entry.getValue(), now));
            }
            logger.debug("writing {}", series);
            long start = System.currentTimeMillis();
            long processing = start - now;
            config.getInfluxDB().write(config.getDatabaseName(), TimeUnit.MILLISECONDS, series.toArray(new Serie[series.size()]));
            logger.info("wrote {} items to influx in {} ms, processing {} ms ", series.size(),
                    (System.currentTimeMillis() - start), processing);
        } catch (Exception e) {
            logger.warn("failed to report to influxdb!", e);
        }
    }

    private Serie timer(String name, Timer timer, long now) {
        Snapshot snapshot = timer.getSnapshot();
        return new Serie.Builder(config.getPrefix() + "." + name)
                .columns("time", "count", "min", "max", "mean", "std-dev"
                        , "50-percentile", "75-percentile", "95-percentile", "99-percentile", "999-percentile"
                        , "one-minute", "five-minute", "fifteen-minute", "mean-rate")
                .values(now,
                        snapshot.size(),
                        convertDuration(snapshot.getMin()),
                        convertDuration(snapshot.getMax()),
                        convertDuration(snapshot.getMean()),
                        convertDuration(snapshot.getStdDev()),
                        convertDuration(snapshot.getMedian()),
                        convertDuration(snapshot.get75thPercentile()),
                        convertDuration(snapshot.get95thPercentile()),
                        convertDuration(snapshot.get99thPercentile()),
                        convertDuration(snapshot.get999thPercentile()),
                        convertRate(timer.getOneMinuteRate()),
                        convertRate(timer.getFiveMinuteRate()),
                        convertRate(timer.getFifteenMinuteRate()),
                        convertRate(timer.getMeanRate()))
                .build();
    }

    private Serie histogram(String name, Histogram histogram, long now) {
        Snapshot snapshot = histogram.getSnapshot();
        return new Serie.Builder(config.getPrefix() + "." + name)
                .columns("time", "count", "min", "max", "mean", "std-dev"
                        , "50-percentile", "75-percentile", "95-percentile", "99-percentile", "999-percentile")
                .values(now,
                        snapshot.size(),
                        convertDuration(snapshot.getMin()),
                        convertDuration(snapshot.getMax()),
                        convertDuration(snapshot.getMean()),
                        convertDuration(snapshot.getStdDev()),
                        convertDuration(snapshot.getMedian()),
                         convertDuration(snapshot.get75thPercentile()),
                        convertDuration(snapshot.get95thPercentile()),
                        convertDuration(snapshot.get99thPercentile()),
                        convertDuration(snapshot.get999thPercentile()))
                .build();
    }

    private Serie counter(String name, Counter counter, long now) {
        return new Serie.Builder(config.getPrefix() + "." + name)
                .columns("time", "count")
                .values(now, counter.getCount())
                .build();
    }

    private Serie gauge(String name, Gauge<?> gauge, long now) {
        Object value = gauge.getValue();
        if (Collection.class.isAssignableFrom(value.getClass())
                || Map.class.isAssignableFrom(value.getClass())) {
            value = value.toString();
        }
        return new Serie.Builder(config.getPrefix() + "." + name)
                .columns("time", "value")
                .values(now, value)
                .build();
    }

    private Serie meter(String name, Metered meter, long now) {
        return new Serie.Builder(config.getPrefix() + "." + name)
                .columns("time", "count", "one-minute", "five-minute", "fifteen-minute", "mean-rate")
                .values(now,
                        meter.getCount(),
                        convertRate(meter.getOneMinuteRate()),
                        convertRate(meter.getFiveMinuteRate()),
                        convertRate(meter.getFifteenMinuteRate()),
                        convertRate(meter.getMeanRate()))
                .build();
    }


}