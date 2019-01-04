package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubVersion;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.ScheduledReporter;
import metrics_influxdb.InfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.UdpInfluxdbProtocol;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import static com.flightstats.hub.app.HubServices.register;
import static com.flightstats.hub.app.HubHost.getLocalName;

@Singleton
public class JVMMetricsService {
    private final Logger logger = LoggerFactory.getLogger(JVMMetricsService.class);

    @Inject
    private MetricRegistry registry;

    @Inject
    private HubVersion hubVersion;

    private ScheduledReporter influxReporter;
    private final String env = HubProperties.getProperty("app.environment", "dev");
    private final boolean metricCollectionEnabled = HubProperties.getProperty("metrics.enabled", false) ||
            System.getProperty("metrics.environment", "dev").equals("test");
    private final String protocolScheme = HubProperties.getProperty("influx.protocolScheme", "http");
    private final String influxHost = HubProperties.getProperty("influx.host", "influxdb");
    private final int influxPort = HubProperties.getProperty("influx.port", 8086);
    private final String influxUser = HubProperties.getProperty("influx.dbUser", "");
    private final String influxPass = HubProperties.getProperty("influx.dbPass", "");
    private final String influxDatabaseName = HubProperties.getProperty("influx.dbName", "hubmain");
    private final int reporterInterval = HubProperties.getProperty("metrics.seconds", 15);
    private final String clusterName = HubProperties.getProperty("cluster.tagName", "single.local");
    private final String metricPrefix = "jvm_";

    @Inject
    JVMMetricsService(MetricRegistry registry, HubVersion hubVersion) {
        this.hubVersion = hubVersion;
        this.registry = registry;
        // custom configurable reporter
        if (metricCollectionEnabled) {
            influxReporter = InfluxdbReporter.forRegistry(registry)
                    .protocol(getProtocol())
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .skipIdleMetrics(false)
                    .tag("role", "hub")
                    .tag("team", "ddt")
                    .tag("env", env)
                    .tag("cluster", clusterName)
                    .tag("host", getHost())
                    .tag("version", getVersion())
                    .build();
            // HubServices.register this as a service
            register(new JVMMetricsIdleService());
        }
    }

    public void start() {
        if (metricCollectionEnabled) {
            logger.info("starting jvm metrics service on {} :// {} : {}", protocolScheme, influxHost, influxPort);
            // collect these metrics
            registry.register(metricPrefix + "gc", new GarbageCollectorMetricSet());
            registry.register(metricPrefix + "thread", new CachedThreadStatesGaugeSet(reporterInterval, TimeUnit.SECONDS));
            registry.register(metricPrefix + "memory", new MemoryUsageGaugeSet());
            // report the metrics every N seconds
            influxReporter.start(reporterInterval, TimeUnit.SECONDS);
        } else {
            logger.info("not starting metrics collection for jvm: disabled");
        }
    }

    @VisibleForTesting InfluxdbProtocol getProtocol() {
        String scheme = protocolScheme;
        // allow use of udp
        if (!"https,udp".contains(scheme.trim().toLowerCase())) {
            logger.warn("Invalid protocol for influxdb reporter - using http");
            scheme = "http";
        }
        return scheme == "http" ? httpProtocol() : udpProtocol();
    }

    private UdpInfluxdbProtocol udpProtocol() {
        return new UdpInfluxdbProtocol(influxHost, influxPort);
    }

    private HttpInfluxdbProtocol httpProtocol() {
        return new HttpInfluxdbProtocol(influxHost, influxPort, influxUser, influxPass, influxDatabaseName);
    }

    @VisibleForTesting SortedSet<String>  getRegisteredMetricNames() {
        return registry.getNames();
    }

    @VisibleForTesting String getHost() {
        try {
            return getLocalName();
        } catch (RuntimeException e) {
            logger.debug("unable to get HubHost.getLocalName() err: {}", e);
            return "unknown";
        }
    }

    @VisibleForTesting String getVersion() {
        return hubVersion.getVersion();
    }

    @VisibleForTesting void stop() {
        influxReporter.close();
    }

    private class JVMMetricsIdleService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            logger.info("shutting down jvm metrics service");
        }

    }
}


