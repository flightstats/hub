package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class SpokeTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTtlEnforcer.class);
    private final String storagePath;
    private final int ttlMinutes;

    @Inject
    public SpokeTtlEnforcer(@Named("spoke.path") String storagePath,
                            @Named("spoke.ttlMinutes") int ttlMinutes) {
        this.storagePath = storagePath;
        this.ttlMinutes = ttlMinutes;
        HubServices.register(new SpokeTtlEnforcerService());
    }

    public void run() {
        try {
            String command = "find " + storagePath + " -mmin +" + ttlMinutes + " -delete";
            logger.debug("running " + command);
            Process process = Runtime.getRuntime().exec(command);
            InputStream stream = new BufferedInputStream(process.getInputStream());
            int waited = process.waitFor();
            logger.debug("waited " + waited);
            if (logger.isTraceEnabled()) {
                byte[] output = ByteStreams.toByteArray(stream);
                logger.trace(new String(output));
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
            return Scheduler.newFixedDelaySchedule(1, 5, TimeUnit.MINUTES);
        }

    }

}
