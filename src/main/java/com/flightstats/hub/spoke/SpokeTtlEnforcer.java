package com.flightstats.hub.spoke;

import com.amazonaws.util.StringUtils;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SpokeTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTtlEnforcer.class);
    private final String storagePath;
    private final int ttlMinutes;

    public SpokeTtlEnforcer() {
        this.storagePath = HubProperties.getProperty("spoke.path", "/spoke");
        this.ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);
        if (HubProperties.getProperty("spoke.enforceTTL", true)) {
            HubServices.register(new SpokeTtlEnforcerService());
        }
    }

    public void run() {
        try {
            String[] command = {"find", storagePath, "-mmin", "+" + ttlMinutes, "-delete"};
            logger.debug("running " + StringUtils.join(" ", command));
            long start = System.currentTimeMillis();
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();
            boolean waited = process.waitFor(1, TimeUnit.MINUTES);
            long time = System.currentTimeMillis() - start;
            if (waited) {
                logger.debug("waited " + waited + " for " + time);
            } else {
                logger.debug("destroying after " + time);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            logger.warn("unable to enforce ttl", e);
        }
    }

    private class SpokeTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
        }

    }

}
