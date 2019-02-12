package com.flightstats.hub.metrics;

import com.flightstats.hub.util.Commander;
import com.google.inject.Inject;
import com.sun.management.UnixOperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Map;

public class CustomMetricsReporter {
    private final static Logger logger = LoggerFactory.getLogger(MetricsRunner.class);
    private final StatsDHandlers statsDHandlers;

    @Inject
    public CustomMetricsReporter(StatsDHandlers statsDHandlers) {
        this.statsDHandlers = statsDHandlers;
//        HubServices.register(new MetricsRunner.MetricsRunnerService());
    }

    long getOpenFiles() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        } else {
            logger.warn("unable to get open files from {}", os.getClass());
            return -1;
        }
    }

    void run() {
        long openFiles = getOpenFiles();
        if (openFiles >= 0) {
            logger.info("open files {}", openFiles);
            statsDHandlers.count("openFiles", openFiles);
        }
    }
}
