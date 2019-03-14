package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.metrics.StatsdReporter;
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class SpokeTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTtlEnforcer.class);
    private final SpokeStore spokeStore;
    private final String storagePath;
    private final int ttlMinutes;

    @Inject
    private ChannelService channelService;

    @Inject
    private StatsdReporter statsdReporter;

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

    private Consumer<ChannelConfig> handleCleanup(AtomicLong evictionCounter) {
        return channel -> {
            int itemsEvicted = 0;
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
                int waitTimeSeconds = 3;
                itemsEvicted += FileUtils.deleteFilesByAge(channelPath, ttlMinutes, waitTimeSeconds);
            }
            evictionCounter.getAndAdd(itemsEvicted);
        };
    }

    private long removeFromChannelByTime(String channelPath, String timePath) {
        String path = channelPath + "/" + timePath;
        int waitTimeSeconds = 3;
        return FileUtils.deleteFiles(path, waitTimeSeconds);
    }

    private void updateOldestItemMetric() {
        Optional<ChannelContentKey> potentialItem = spokeContentDao.getOldestItem(spokeStore);
        long oldestItemAgeMS = potentialItem.isPresent() ? potentialItem.get().getAgeMS() : 0;
        statsdReporter.gauge(buildMetricName("age", "oldest"), oldestItemAgeMS);
    }

    private String buildMetricName(String... elements) {
        String prefix = String.format("spoke.%s", spokeStore);
        Stream<String> stream = Stream.concat(Stream.of(prefix), Arrays.stream(elements));
        return stream.collect(Collectors.joining("."));
    }

    private class SpokeTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                long start = System.currentTimeMillis();
                AtomicLong evictionCounter = new AtomicLong(0);
                logger.info("running ttl cleanup");
                TtlEnforcer.enforce(storagePath, channelService, handleCleanup(evictionCounter));
                updateOldestItemMetric();
                statsdReporter.gauge(buildMetricName("evicted"), evictionCounter.get());
                long runtime = (System.currentTimeMillis() - start);
                logger.info("completed ttl cleanup {}", runtime);
                statsdReporter.gauge(buildMetricName("ttl", "enforcer", "runtime"), runtime);
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
