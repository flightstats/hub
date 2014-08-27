package com.flightstats.hub.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Builder;
import org.influxdb.InfluxDB;

import java.util.concurrent.TimeUnit;

@Builder
@Getter
@ToString
public class InfluxConfig  {

    private final Clock clock = Clock.defaultClock();
    @NonNull private final String prefix;
    @NonNull private final MetricRegistry registry;
    @NonNull private final TimeUnit rateUnit;
    @NonNull private final TimeUnit durationUnit;
    @NonNull private final MetricFilter filter;
    @NonNull private final InfluxDB influxDB;
    @NonNull private final String databaseName;
}