package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Singleton
public class GCRunner {
    private final static Logger logger = LoggerFactory.getLogger(GCRunner.class);
    private final int gcMinutes;

    @Inject
    public GCRunner(ChannelService channelService) {
        this.gcMinutes = HubProperties.getProperty("hub.gcMinutes", 60);
        if (HubProperties.getProperty("hub.runGC", false)) {
            HubServices.register(new GCRunnerService(), HubServices.TYPE.PRE_START);
        }
    }

    public void run() {
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
