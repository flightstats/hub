package com.flightstats.hub.time;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.util.Commander;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NtpMonitor {

    private final static Logger logger = LoggerFactory.getLogger(NtpMonitor.class);

    private final MetricsService metricsService;
    private final int minPostTimeMillis;
    private final int maxPostTimeMillis;
    private double primaryOffset;

    @Inject
    public NtpMonitor(MetricsService metricsService, HubProperties hubProperties) {
        this.metricsService = metricsService;
        this.minPostTimeMillis = hubProperties.getProperty("app.minPostTimeMillis", 5);
        this.maxPostTimeMillis = hubProperties.getProperty("app.maxPostTimeMillis", 1000);

        if (hubProperties.getProperty("app.runNtpMonitor", true)) {
            HubServices.register(new TimeMonitorService());
        } else {
            logger.info("not running NtpMonitor");
        }
    }

    double parseClusterRange(List<String> lines) {
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

    double parsePrimary(List<String> lines) {
        List<Double> servers = new ArrayList<>();
        for (int i = 2; i <= Math.min(3, lines.size() - 1); i++) {
            String line = lines.get(i);
            if (line.startsWith("*") || line.startsWith("+")) {
                double primary = parseLine(line);
                logger.info("primary {}", primary);
                servers.add(primary);
            }
        }
        return servers.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    private double parseLine(String line) {
        String[] split = StringUtils.split(line, " ");
        return Double.parseDouble(split[split.length - 2]);
    }

    public int getPostTimeBuffer() {
        return Math.min(maxPostTimeMillis, (int) (minPostTimeMillis + primaryOffset));
    }


    private class TimeMonitorService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                List<String> lines = Commander.runLines(new String[]{"ntpq", "-p"}, 10);
                double delta = parseClusterRange(lines);
                metricsService.gauge("ntp", delta, "ntpType:clusterTimeDelta");
                double primary = parsePrimary(lines);
                primaryOffset = Math.abs(primary);
                metricsService.gauge("ntp", primaryOffset, "ntpType:primaryTimeDelta");
                logger.info("ntp cluster {} primary {}", delta, primary);
            } catch (Exception e) {
                logger.info("unable to exec", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
        }
    }
}
