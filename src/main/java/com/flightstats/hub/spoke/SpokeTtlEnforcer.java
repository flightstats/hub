package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.FileUtil;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class SpokeTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTtlEnforcer.class);
    private final String storagePath = HubProperties.getProperty("spoke.path", "/spoke");
    private final int ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);
    @Inject
    private ChannelService channelService;

    @Inject
    public SpokeTtlEnforcer() {
        if (HubProperties.getProperty("spoke.enforceTTL", true)) {
            HubServices.register(new SpokeTtlEnforcerService());
            HubServices.register(new SpokeTtlEnforcerInitialService());
        }
    }

    private Consumer<ChannelConfig> handleCleanup() {
        return channel -> {
            String channelPath = storagePath + "/" + channel.getName();
            if (channel.isReplicating()) {
                FileUtil.runCommand(new String[]{"find", channelPath, "-mmin", "+" + ttlMinutes, "-delete"}, 1);
            } else {
                DateTime ttlDateTime = TimeUtil.stable().minusMinutes(ttlMinutes + 1);
                for (int i = 0; i < 3; i++) {
                    FileUtil.runCommand(new String[]{"rm", "-rf", channelPath + "/" + TimeUtil.minutes(ttlDateTime.minusMinutes(i))}, 1);
                    FileUtil.runCommand(new String[]{"rm", "-rf", channelPath + "/" + TimeUtil.hours(ttlDateTime.minusHours(i + 1))}, 5);
                }
            }
        };
    }

    private class SpokeTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                long start = System.currentTimeMillis();
                logger.info("running ttl cleanup");
                TtlEnforcer.enforce(storagePath, channelService, handleCleanup());
                logger.info("completed ttl cleanup {}", (System.currentTimeMillis() - start));
            } catch (Exception e) {
                logger.info("issue cleaning up spoke", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
        }

    }

    private class SpokeTtlEnforcerInitialService extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            logger.info("performing Spoke cleanup");
            FileUtil.runCommand(new String[]{"find", storagePath, "-mmin", "+" + ttlMinutes, "-delete"}, 10 * 60);
            logger.info("completed Spoke cleanup");
        }

        @Override
        protected void shutDown() throws Exception {

        }
    }
}
