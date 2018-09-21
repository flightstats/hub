package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.util.FileUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
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
    private final SpokeStore spokeStore;
    private final String storagePath;
    private final int ttlMinutes;
    private long itemsEvicted = 0;

    @Inject
    private ChannelService channelService;

    @Inject
    private MetricsService metricsService;

    @Inject
    private SpokeContentDao spokeContentDao;

    public SpokeTtlEnforcer(SpokeStore spokeStore) {
        this.spokeStore = spokeStore;
        this.storagePath = HubProperties.getSpokePath(spokeStore);
        this.ttlMinutes = HubProperties.getSpokeTtlMinutes(spokeStore) + 1;
        if (HubProperties.getProperty("spoke.enforceTTL", true)) {
            HubServices.register(new SpokeTtlEnforcerService());
        }
    }

    private Consumer<ChannelConfig> handleCleanup() {
        return channel -> {
            String channelPath = storagePath + "/" + channel.getDisplayName();
            if (channel.isLive()) {
                DateTime ttlDateTime = TimeUtil.stable().minusMinutes(ttlMinutes + 1);
                for (int i = 0; i < 2; i++) {
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.minutes(ttlDateTime.minusMinutes(i)));
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.hours(ttlDateTime.minusHours(i + 1)));
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.days(ttlDateTime.minusDays(i + 1)));
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.months(ttlDateTime.minusMonths(i + 1)));
                }
            } else {
                itemsEvicted += FileUtils.deleteFilesByAge(channelPath, ttlMinutes, 3);
            }
        };
    }

    private long removeFromChannelByTime(String channelPath, String timePath) {
        String path = channelPath + "/" + timePath;
        return FileUtils.deleteFiles(path, 3);
    }

    private void updateOldestItemMetric() {
        Long oldestItemAgeMS = null;
        Optional<ChannelContentKey> potentialItem = spokeContentDao.getOldestItem(spokeStore);
        if (potentialItem.isPresent()) {
            oldestItemAgeMS = potentialItem.get().getAgeMS();
        }
        if (oldestItemAgeMS == null) {
            oldestItemAgeMS = 0L;
        }
        metricsService.gauge("spoke." + spokeStore + ".age.oldest", oldestItemAgeMS);
    }

    private void updateItemsEvictedMetric() {
        metricsService.gauge("spoke." + spokeStore + ".evicted", itemsEvicted);
        itemsEvicted = 0;
    }

    private class SpokeTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                long start = System.currentTimeMillis();
                logger.info("running ttl cleanup");
                TtlEnforcer.enforce(storagePath, channelService, handleCleanup());
                updateOldestItemMetric();
                updateItemsEvictedMetric();
                long runtime = (System.currentTimeMillis() - start);
                logger.info("completed ttl cleanup {}", runtime);
                metricsService.gauge("spoke." + spokeStore + ".ttl.enforcer.runtime", runtime);
            } catch (Exception e) {
                logger.info("issue cleaning up spoke", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.MINUTES);
        }

    }

}
