package com.flightstats.hub.time;


import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class TimeMonitor {

    private final static Logger logger = LoggerFactory.getLogger(TimeMonitor.class);

    private final HostedGraphiteSender sender;

    @Inject
    public TimeMonitor(HostedGraphiteSender sender) {
        this.sender = sender;
        HubServices.register(new TimeMonitorService(), HubServices.TYPE.POST_START);
    }

    static double parseClusterRange(List<String> lines) {
        logger.trace("ntpq {}", lines);
        double maxPositive = 0;
        double maxNegative = 0;
        for (String line : lines) {
            if (line.contains("hub-v2")) {
                logger.debug("ntp hub peer {}", line);
                String[] split = StringUtils.split(line, " ");
                double offset = Double.parseDouble(split[split.length - 2]);
                if (offset > 0) {
                    maxPositive = Math.max(maxPositive, offset);
                } else {
                    maxNegative = Math.max(maxNegative, Math.abs(offset));
                }
            }
        }
        return maxNegative + maxPositive;
    }

    private void run() {
        try {
            Process process = new ProcessBuilder("ntpq", "-p").start();
            List<String> lines = IOUtils.readLines(process.getInputStream());
            double clusterRange = parseClusterRange(lines);
            sender.send("clusterTimeDelta", clusterRange);
        } catch (Exception e) {
            logger.info("unable to exec", e);
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
