package com.flightstats.hub.metrics;

import com.flightstats.hub.app.*;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.ScheduledReporter;
import com.google.inject.Singleton;
import metrics_influxdb.InfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.HttpInfluxdbProtocol;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import metrics_influxdb.UdpInfluxdbProtocol;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import static com.flightstats.hub.app.HubServices.register;

@Singleton
public class JVMMetricsService {
    private final Logger logger = LoggerFactory.getLogger(JVMMetricsService.class);

    @Inject
    private MetricRegistry registry;

    private final String protocolType = HubProperties.getProperty("jvmMetrics.protocol", "http");
    private final String influxHost = HubProperties.getProperty("jvmMetrics.host", "influxdb");
    private final int influxPort = HubProperties.getProperty("jvmMetrics.jvmPort", 8086);
    private final String influxUser = HubProperties.getProperty("jvmMetrics.dbUser", "");
    private final String influxPass = HubProperties.getProperty("jvmMetrics.dbPass", "");
    private final String influxDatabaseName = HubProperties.getProperty("jvmMetrics.dbName", "hubmain");
    private final int reporterInterval = HubProperties.getProperty("jvmMetrics.seconds", 15);

    private final String metricPrefix = "jvm_";
    private ScheduledReporter influxDBReporter;

    @Inject
    JVMMetricsService(MetricRegistry registry) {
        this.registry = registry;
        register(new JVMMetricsIdleService());
    }

    public void start() {
        logger.info("starting jvm metrics service on {} :// {} : {}", protocolType, influxHost, influxPort);
        // custom configurable reporter
        influxDBReporter = InfluxdbReporter.forRegistry(registry)
                .protocol(getProtocol())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .skipIdleMetrics(false)
                .tag("role", "hub")
                .tag("team", "ddt")
                .tag("env", getEnvironment())
                .tag("cluster", getCluster())
                .tag("host", getHost())
                .tag("version", getVersion())
                .build();
        // collect these metrics
        registry.register(metricPrefix + "gc", new GarbageCollectorMetricSet());
        registry.register(metricPrefix + "thread", new CachedThreadStatesGaugeSet(15, TimeUnit.SECONDS));
        registry.register(metricPrefix + "memory", new MemoryUsageGaugeSet());
        influxDBReporter.start(reporterInterval, TimeUnit.SECONDS);
    }

    InfluxdbProtocol getProtocol() {
        String protocol = protocolType;
        // allow use of udp
        if (!"https,udp".contains(protocol.trim().toLowerCase())) {
            logger.warn("Invalid protocol for influxdb reporter - using http");
            protocol = "http";
        }
        return protocol == "http" ? httpProtocol() : udpProtocol();
    }

    private UdpInfluxdbProtocol udpProtocol() {
        return new UdpInfluxdbProtocol(influxHost, influxPort);
    }

    private HttpInfluxdbProtocol httpProtocol() {
        return new HttpInfluxdbProtocol(influxHost, influxPort, influxUser, influxPass, influxDatabaseName);
    }

    @VisibleForTesting public SortedSet<String>  getRegisteredMetricNames() {
        return registry.getNames();
    }

    private String getEnvironment() {
        return HubProperties.getProperty("app.environment", "dev");
    }

    private String getCluster() {
        return HubProperties.getProperty("cluster.tagname", "single.local");
    }

    private String getHost() {
        try {
            return HubHost.getLocalName();
        } catch (RuntimeException e) {
            logger.debug("unable to get HubHost.getLocalName() err: {}", e);
            return "unknown-host";
        }
    }

    private String getVersion() {
        HubVersion hubVersion = HubProvider.getInstance(HubVersion.class);
        return hubVersion.getVersion();
//        return "test";
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


