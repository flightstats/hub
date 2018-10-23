package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.util.Commander;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.sun.management.UnixOperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
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
    public MetricsRunner(MetricsService metricsService, HubProperties hubProperties) {
        this.metricsService = metricsService;
        this.seconds = hubProperties.getProperty("metrics.seconds", 30);

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
            for (StackTraceElement element : value) {
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
        }
    }


    private class MetricsRunnerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, seconds, TimeUnit.SECONDS);
        }

    }

}
