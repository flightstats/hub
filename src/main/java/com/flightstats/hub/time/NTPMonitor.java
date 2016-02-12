package com.flightstats.hub.time;


import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.metrics.MetricsSender;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NTPMonitor {

    private final static Logger logger = LoggerFactory.getLogger(NTPMonitor.class);

    @Inject
    private MetricsSender sender;
    @Inject
    private TimeAdjuster timeAdjuster;

    private final int minPostTimeMillis;
    private final int maxPostTimeMillis;
    private double delta;
    private double primaryOffset;

    public NTPMonitor() {
        HubServices.register(new TimeMonitorService(), HubServices.TYPE.PRE_START);
        minPostTimeMillis = HubProperties.getProperty("app.minPostTimeMillis", 5);
        maxPostTimeMillis = HubProperties.getProperty("app.maxPostTimeMillis", 1000);
    }

    static double parseClusterRange(List<String> lines) {
        double maxPositive = 0;
        double maxNegative = 0;
        for (String line : lines) {
            if (line.contains("hub")) {
                double offset = parseLine(line);
                if (offset > 0) {
                    maxPositive = Math.max(maxPositive, offset);
                } else {
                    maxNegative = Math.max(maxNegative, Math.abs(offset));
                }
            }
        }
        return maxNegative + maxPositive;
    }

    static double parsePrimary(List<String> lines) {
        //todo - gfm - 2/12/16 - this may want to verify the number of servers found
        List<Double> servers = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("hub")) {
                //ignore
            } else if (line.startsWith("*") || line.startsWith("+")) {
                double primary = parseLine(line);
                logger.info("primary {}", primary);
                servers.add(primary);
            }
        }
        return servers.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    private static double parseLine(String line) {
        String[] split = StringUtils.split(line, " ");
        return Double.parseDouble(split[split.length - 2]);
    }

    @Trace(metricName = "NtpMonitor", dispatcher = true)
    public void run() {
        try {
            Process process = new ProcessBuilder("ntpq", "-p").start();
            List<String> lines = IOUtils.readLines(process.getInputStream());
            delta = parseClusterRange(lines);
            sender.send("clusterTimeDelta", delta);
            double primary = parsePrimary(lines);
            timeAdjuster.setOffset((int) primary);
            primaryOffset = Math.abs(primary);
            sender.send("primaryTimeDelta", primary);
            newRelic(delta);
        } catch (Exception e) {
            logger.info("unable to exec", e);
        }
    }

    public int getPostTimeBuffer() {
        return Math.min(maxPostTimeMillis, (int) (minPostTimeMillis + primaryOffset));
    }

    public void newRelic(double delta) {
        NewRelic.addCustomParameter("clusterTimeDelta", delta);
        NewRelic.recordResponseTimeMetric("Custom/ClusterTimeDeltaResponse", (long) delta);
        if (delta >= 5.0) {
            NewRelic.noticeError("cluster time delta too high");
        }
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
