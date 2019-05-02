package com.flightstats.hub.time;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.util.Commander;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class NtpMonitor {

    private final StatsdReporter statsdReporter;
    private final AppProperties appProperties;
    private double primaryOffset;

    @Inject
    public NtpMonitor(StatsdReporter statsdReporter, AppProperties appProperties) {
        this.statsdReporter = statsdReporter;
        this.appProperties = appProperties;

        if (this.appProperties.isNtpMonitorEnabled()) {
            HubServices.register(new TimeMonitorService());
        } else {
            log.info("not running NtpMonitor");
        }
    }

    static double parseClusterRange(List<String> lines) {
        double maxPositive = 0;
        double maxNegative = 0;
        for (int i = 4; i < lines.size(); i++) {
            String line = lines.get(i);
            double offset = parseLine(line);
            if (offset > 0) {
                maxPositive = Math.max(maxPositive, offset);
            } else {
                maxNegative = Math.max(maxNegative, Math.abs(offset));
            }
        }
        return maxNegative + maxPositive;
    }

    static double parsePrimary(List<String> lines) {
        List<Double> servers = new ArrayList<>();
        for (int i = 2; i <= Math.min(3, lines.size() - 1); i++) {
            String line = lines.get(i);
            if (line.startsWith("*") || line.startsWith("+")) {
                double primary = parseLine(line);
                log.info("primary {}", primary);
                servers.add(primary);
            }
        }
        return servers.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    private static double parseLine(String line) {
        String[] split = StringUtils.split(line, " ");
        return Double.parseDouble(split[split.length - 2]);
    }

    private void run() {
        try {
            List<String> lines = Commander.runLines(new String[]{"ntpq", "-p"}, 10);
            double delta = parseClusterRange(lines);
            statsdReporter.gauge("ntp", delta, "ntpType:clusterTimeDelta");
            double primary = parsePrimary(lines);
            primaryOffset = Math.abs(primary);
            statsdReporter.gauge("ntp", primaryOffset, "ntpType:primaryTimeDelta");
            log.info("ntp cluster {} primary {}", delta, primary);
        } catch (Exception e) {
            log.info("unable to exec", e);
        }
    }

    public int getPostTimeBuffer() {
        return Math.min(this.appProperties.getMaxPostTimeMillis(),
                (int) (this.appProperties.getMinPostTimeMillis() + primaryOffset));
    }


    private class TimeMonitorService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
        }
    }
}
