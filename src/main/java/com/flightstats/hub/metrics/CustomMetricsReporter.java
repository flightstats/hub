package com.flightstats.hub.metrics;

import javax.inject.Inject;
import com.sun.management.UnixOperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@Slf4j
public class CustomMetricsReporter {
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
            log.error("unable to get open files from {}", os.getClass());
            return -1;
        }
    }

    void run() {
        long openFiles = getOpenFiles();
        if (openFiles >= 0) {
            log.debug("open files {}", openFiles);
            statsdReporter.count("openFiles", openFiles);
        }
    }
}
