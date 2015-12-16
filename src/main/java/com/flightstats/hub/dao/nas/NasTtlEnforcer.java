package com.flightstats.hub.dao.nas;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.FileUtil;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class NasTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(NasTtlEnforcer.class);
    @Inject
    private final ChannelService channelService;
    private final String contentPath = NasUtil.getContentPath();

    @Inject
    public NasTtlEnforcer(ChannelService channelService) {
        this.channelService = channelService;
        HubServices.register(new NasTtlEnforcerService());
    }

    private Consumer<ChannelConfig> handleCleanup() {
        return channel -> {
            String channelPath = contentPath + "/" + channel.getName();
            DateTime ttlDateTime = TimeUtil.stable().minusDays((int) channel.getTtlDays() + 1);
            FileUtil.runCommand(new String[]{"rm", "-rf", channelPath + "/" + TimeUtil.days(ttlDateTime)}, 60);
        };
    }

    private class NasTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            TtlEnforcer.enforce(contentPath, channelService, handleCleanup());
        }

        @Override
        protected Scheduler scheduler() {
            int hours = (int) TimeUnit.HOURS.toMinutes(12);
            int minutes = new Random().nextInt(hours) + hours;
            logger.info("running ttl enforcer at {} minute offset", minutes);
            return Scheduler.newFixedDelaySchedule(1, minutes, TimeUnit.MINUTES);
        }

    }
}
