package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class GCRunner {
    private final static Logger logger = LoggerFactory.getLogger(GCRunner.class);
    private final int gcMinutes;

    @Inject
    public GCRunner(HubProperties hubProperties) {
        this.gcMinutes = hubProperties.getProperty("hub.gcMinutes", 60);

        if (hubProperties.getProperty("hub.runGC", false)) {
            HubServices.register(new GCRunnerService());
        }
    }

    private void run() {
        logger.info("running GC");
        long start = System.currentTimeMillis();
        System.gc();
        logger.info("ran GC {}", (System.currentTimeMillis() - start));
    }

    private class GCRunnerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(gcMinutes, gcMinutes, TimeUnit.MINUTES);
        }

    }

}
