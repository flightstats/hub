package com.flightstats.hub.dao.nas;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NasTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(NasTtlEnforcer.class);
    private final ChannelService channelService;
    private final NasContentService nasContentService;

    @Inject
    public NasTtlEnforcer(ChannelService channelService, NasContentService nasContentService) {
        this.channelService = channelService;
        this.nasContentService = nasContentService;
        HubServices.register(new NasTtlEnforcerService(), HubServices.TYPE.PRE_START);
    }

    public void run() {
        try {
            for (ChannelConfig channel : channelService.getChannels()) {
                enforceTtl(channel);
            }
        } catch (Exception e) {
            logger.warn("unable to enforce ttl", e);
        }
    }

    private void enforceTtl(ChannelConfig channel) {
        try {
            DateTime dateTime = TimeUtil.now()
                    .minusDays((int) channel.getTtlDays())
                    .minusMinutes(2);
            nasContentService.enforceTtl(channel.getName(), dateTime);
        } catch (Exception e) {
            logger.warn("unable to enforce ttl for {}", channel);
        }
    }


    private class NasTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            int minutes = new Random().nextInt(24 * 60) + 1;
            logger.info("running ttle enforcer at {} minute offset", minutes);
            return Scheduler.newFixedDelaySchedule(1, minutes, TimeUnit.MINUTES);
        }

    }
}
