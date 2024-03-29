package com.flightstats.hub.spoke;

import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.TtlEnforcer;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.util.FileUtils;
import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SpokeTtlEnforcer {

    private final ChannelService channelService;
    private final SpokeContentDao spokeContentDao;
    private final StatsdReporter statsdReporter;
    private final SpokeStore spokeStore;
    private final TtlEnforcer ttlEnforcer;
    private final String storagePath;
    private final int ttlMinutes;

    @Inject
    public SpokeTtlEnforcer(SpokeStore spokeStore,
                            ChannelService channelService,
                            SpokeContentDao spokeContentDao,
                            StatsdReporter statsdReporter,
                            SpokeProperties spokeProperties,
                            TtlEnforcer ttlEnforcer) {
        this.spokeStore = spokeStore;
        this.channelService = channelService;
        this.spokeContentDao = spokeContentDao;
        this.statsdReporter = statsdReporter;
        this.storagePath = spokeProperties.getPath(spokeStore);
        this.ttlMinutes = spokeProperties.getTtlMinutes(spokeStore);
        this.ttlEnforcer = ttlEnforcer;
    }

    private Consumer<ChannelConfig> handleCleanup(AtomicLong evictionCounter) {
        return channel -> {
            int itemsEvicted = 0;
            final String channelPath = storagePath + "/" + channel.getDisplayName();

            if (!channel.isLive() || spokeStore.equals(SpokeStore.READ)) {
                int waitTimeSeconds = 3;
                itemsEvicted += FileUtils.deletePathsByAge(channelPath, ttlMinutes, waitTimeSeconds);
                FileUtils.deleteEmptyDirectories(channelPath, waitTimeSeconds);
            }
            else {
                final DateTime ttlDateTime = TimeUtil.stable().minusMinutes(ttlMinutes + 1);
                for (int i = 0; i < 2; i++) {
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.minutes(ttlDateTime.minusMinutes(i)));
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.hours(ttlDateTime.minusHours(i + 1)));
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.days(ttlDateTime.minusDays(i + 1)));
                    itemsEvicted += removeFromChannelByTime(channelPath, TimeUtil.months(ttlDateTime.minusMonths(i + 1)));
                }
            }
            evictionCounter.getAndAdd(itemsEvicted);
        };
    }

    private long removeFromChannelByTime(String channelPath, String timePath) {
        final String path = channelPath + "/" + timePath;
        final int waitTimeSeconds = 3;
        return FileUtils.deletePaths(path, waitTimeSeconds);
    }

    private void updateOldestItemMetric() {
        final Optional<ChannelContentKey> potentialItem = spokeContentDao.getOldestItem(spokeStore);
        final long oldestItemAgeMS = potentialItem.map(ChannelContentKey::getAgeMS).orElse(0L);
        statsdReporter.gauge(buildMetricName("age", "oldest"), oldestItemAgeMS);
    }

    private String buildMetricName(String... elements) {
        final String prefix = String.format("spoke.%s", spokeStore);
        final Stream<String> stream = Stream.concat(Stream.of(prefix), Arrays.stream(elements));
        return stream.collect(Collectors.joining("."));
    }

    void cleanup() {
        try {
            long start = System.currentTimeMillis();
            AtomicLong evictionCounter = new AtomicLong(0);

            log.debug("running ttl cleanup");
            ttlEnforcer.deleteFilteredPaths(storagePath, channelService, handleCleanup(evictionCounter));
            updateOldestItemMetric();
            statsdReporter.gauge(buildMetricName("evicted"), evictionCounter.get());

            long runtime = (System.currentTimeMillis() - start);
            log.info("completed ttl cleanup {}", runtime);
            statsdReporter.gauge(buildMetricName("ttl", "enforcer", "runtime"), runtime);

        } catch (Exception e) {
            log.error("issue cleaning up spoke", e);
        }
    }
}
