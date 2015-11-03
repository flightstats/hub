package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.sun.management.UnixOperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.TimeUnit;

@Singleton
public class MetricsRunner {
    private final static Logger logger = LoggerFactory.getLogger(MetricsRunner.class);
    private final int seconds;
    private final MetricsSender metricsSender;

    @Inject
    public MetricsRunner(MetricsSender metricsSender) {
        this.metricsSender = metricsSender;
        this.seconds = HubProperties.getProperty("metrics.seconds", 30);
        HubServices.register(new MetricsRunnerService());
    }

    public void run() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            long openFiles = ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
            logger.info("open files {}", openFiles);
            metricsSender.send("openFiles", openFiles);
            newRelic(openFiles);
        } else {
            logger.warn("unable to get open files from {}", os.getClass());
        }
    }

    @Trace(metricName = "MetricsRunner", dispatcher = true)
    public void newRelic(long openFiles) {
        NewRelic.addCustomParameter("openFileCount", openFiles);
        NewRelic.recordResponseTimeMetric("Custom/OpenFileCount", openFiles);
        if (openFiles >= 2000) {
            NewRelic.noticeError("too many open files");
        }
    }

    private class MetricsRunnerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, seconds, TimeUnit.SECONDS);
        }

    }

}
