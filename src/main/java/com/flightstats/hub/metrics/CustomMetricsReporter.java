package com.flightstats.hub.metrics;

import com.google.inject.Inject;
import com.sun.management.UnixOperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class CustomMetricsReporter {
    private final static Logger logger = LoggerFactory.getLogger(CustomMetricsReporter.class);
    private final StatsdReporter statsdReporter;

    @Inject
    public CustomMetricsReporter(StatsdReporter statsdReporter) {
        this.statsdReporter = statsdReporter;
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
            statsdReporter.count("openFiles", openFiles);
        }
    }
}
