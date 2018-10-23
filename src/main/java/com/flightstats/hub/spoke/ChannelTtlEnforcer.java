package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.Commander;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class ChannelTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(ChannelTtlEnforcer.class);
    private final String spokePath;
    private final ChannelService channelService;

    @Inject
    public ChannelTtlEnforcer(ChannelService channelService, HubProperties hubProperties) {
        this.channelService = channelService;
        this.spokePath = hubProperties.getSpokePath(SpokeStore.WRITE);

        if (hubProperties.getProperty("channel.enforceTTL", false)) {
            HubServices.register(new ChannelTtlEnforcerService());
        }
    }

    private Consumer<ChannelConfig> handleCleanup() {
        return channel -> {
            if (channel.getTtlDays() > 0) {
                String channelPath = spokePath + "/" + channel.getDisplayName();
                DateTime channelTTL = TimeUtil.stable().minusDays((int) channel.getTtlDays());
                for (int i = 0; i < 3; i++) {
                    Commander.run(new String[]{"rm", "-rf", channelPath + "/" + TimeUtil.days(channelTTL.minusDays(i))}, 1);
                }
            }
        };
    }

    private class ChannelTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                long start = System.currentTimeMillis();
                logger.info("running channel cleanup");
                TtlEnforcer.enforce(spokePath, channelService, handleCleanup());
                logger.info("completed channel cleanup {}", (System.currentTimeMillis() - start));
            } catch (Exception e) {
                logger.info("issue cleaning up channels in spoke", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.DAYS);
        }

    }

}
