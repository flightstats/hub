package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.util.Commander;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class MetricsRunner {
    private final static Logger logger = LoggerFactory.getLogger(MetricsRunner.class);
    private final int seconds;
    private final MetricsService metricsService;

    @Inject
    public MetricsRunner(MetricsService metricsService) {
        this.metricsService = metricsService;
        this.seconds = HubProperties.getProperty("metrics.seconds", 30);
        HubServices.register(new MetricsRunnerService());
    }

    static long getOpenFiles() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        } else {
            logger.warn("unable to get open files from {}", os.getClass());
            return -1;
        }
    }

    static String logFilesInfo() {
        String info = "";
        logger.info("logFilesInfo starting");
        info += "lsof -b -cjava : \r\n";
        info += Commander.run(new String[]{"lsof", "-b", "-cjava"}, 60);
        info += "thread dump \r\n";
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            StackTraceElement[] value = entry.getValue();
            info += entry.getKey().getName() + " : \r\n";
            for (int i = 0; i < value.length; i++) {
                StackTraceElement element = value[i];
                info += "\t" + element.toString() + "\r\n";
            }
        }
        logger.info("logFilesInfo completed");
        return info;
    }

    private void run() {
        long openFiles = getOpenFiles();
        if (openFiles >= 0) {
            logger.info("open files {}", openFiles);
            metricsService.count("openFiles", openFiles);
            newRelic(openFiles);
        }
    }

    @Trace(metricName = "MetricsRunner", dispatcher = true)
    private void newRelic(long openFiles) {
        NewRelic.addCustomParameter("openFileCount", openFiles);
        NewRelic.recordResponseTimeMetric("Custom/OpenFileCount", openFiles);
        if (openFiles >= 1000) {
            NewRelic.noticeError("too many open files");
            logger.info(logFilesInfo());
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
