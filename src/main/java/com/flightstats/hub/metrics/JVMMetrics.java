package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubVersion;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricFilter;
import com.google.inject.Inject;
import com.kickstarter.dropwizard.metrics.influxdb.io.Sender;
import com.kickstarter.dropwizard.metrics.influxdb.io.InfluxDbWriter;
import com.kickstarter.dropwizard.metrics.influxdb.io.InfluxDbTcpWriter;
import com.kickstarter.dropwizard.metrics.influxdb.transformer.DropwizardMeasurementParser;
import com.kickstarter.dropwizard.metrics.influxdb.transformer.DropwizardTransformer;
import com.kickstarter.dropwizard.metrics.influxdb.transformer.TaggedPattern;
import com.kickstarter.dropwizard.metrics.influxdb.InfluxDbMeasurementReporter;
import io.dropwizard.util.Duration;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class JVMMetrics {
    private final Logger logger = LoggerFactory.getLogger(JVMMetrics.class);
    private MetricRegistry registry;
    private InfluxDbWriter tcpWriter;
    private Sender influxdbSender;
    private InfluxDbMeasurementReporter influxdbReporter;
    private DropwizardTransformer influxdbMetricsFormatter;
    private DropwizardMeasurementParser defaultMetricParser;
    private ScheduledExecutorService executor;

    private final String metricPrefix = "jvm_";
    private String influxdbHost = HubProperties.getProperty("influxdb.endpoint", "http://localhost:8086");
    private int influxdbPort = HubProperties.getProperty("influxdb.port", 8086);
    private final int reporterInterval = HubProperties.getProperty("metrics.seconds", 15);
    private final Duration influxdbRequestTimeout = Duration.seconds(30);
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"))

    public void startMetricRegistry() {
        logger.info("starting jvm metrics service");
        registry = new MetricRegistry();
        registry.register( metricPrefix + "gc", new GarbageCollectorMetricSet());
        registry.register(metricPrefix + "thread", new CachedThreadStatesGaugeSet(15, TimeUnit.SECONDS));
        registry.register(metricPrefix + "memory", new MemoryUsageGaugeSet());

        executor = Executors.newSingleThreadScheduledExecutor();
        tcpWriter = new InfluxDbTcpWriter(influxdbHost, influxdbPort, influxdbRequestTimeout);
        influxdbSender = new Sender(tcpWriter);
        defaultMetricParser = DropwizardMeasurementParser.withTemplates(getDefaultMetricPattern());
        influxdbMetricsFormatter = new DropwizardTransformer(
                getGlobalTags(),
                defaultMetricParser,
                false,
                false,
                TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS
        );
        influxdbReporter = new InfluxDbMeasurementReporter(
                influxdbSender,
                registry,
                MetricFilter.ALL,
                TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS,
                clock,
                influxdbMetricsFormatter,
                executor
        );
        influxdbReporter.start(reporterInterval, TimeUnit.SECONDS);
    }

    @VisibleForTesting public SortedSet<String>  getRegisteredMetricNames() {
        return registry.getNames();
    }

    private String getEnvironment() {
        return HubProperties.getProperty("app.environment", "dev");
    }

    private String getCluster() {
        return HubProperties.getProperty("cluster.tagname", "single.hub");
    }

    private String getHost() {
        return HubHost.getLocalHttpNameUri();
    }

    private String getVersion() {
//        HubVersion hubVersion = HubProvider.getInstance(HubVersion.class);
//        return hubVersion.getVersion();
        return "test";
    }

    private Map<String, TaggedPattern> getDefaultMetricPattern() {
        Map<String, TaggedPattern> metricsPattern = new HashMap<>();
        metricsPattern.put("becauseIhaveToo", new TaggedPattern("^*", Collections.singletonList("dont.tag.except.default")));
        return metricsPattern;
    }

    private Map<String, String> getGlobalTags() {
//      cluster env host role team version
        Map<String, String> globalTags = new HashMap<>();
        globalTags.put("role", "hub");
        globalTags.put("team", "ddt");
        globalTags.put("env", getEnvironment());
        globalTags.put("cluster", getCluster());
        globalTags.put("host", getHost());
        globalTags.put("version", getVersion());
        return globalTags;
    }

    public void shutDown() {
        logger.info("shutting down jvm metrics service");
        influxdbReporter.close();
    }
}


