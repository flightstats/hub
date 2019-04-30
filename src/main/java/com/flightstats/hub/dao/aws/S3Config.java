package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.DistributedAsyncLockRunner;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

@Slf4j
public class S3Config {

    // S3 limits max lifecycle rules to 1000. 10 rules are made available for setting lifecycle rules from infrastructure code.
    private static final Integer S3_LIFECYCLE_RULES_AVAILABLE = 990;

    private final DistributedAsyncLockRunner distributedLockRunner;
    private final Dao<ChannelConfig> channelConfigDao;
    private final String s3BucketName;
    private final HubS3Client s3Client;
    private ChannelService channelService;

    @Inject
    public S3Config(HubS3Client s3Client, S3BucketName s3BucketName, DistributedAsyncLockRunner distributedLockRunner,
                    @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao, ChannelService channelService) {
        this.s3Client = s3Client;
        this.distributedLockRunner = distributedLockRunner;
        this.channelConfigDao = channelConfigDao;
        this.channelService = channelService;
        this.s3BucketName = s3BucketName.getS3BucketName();
        HubServices.register(new S3ConfigInit());
    }

    private void run() {
        try {
            doWork();
        } catch (Exception e) {
            log.warn("unable to update config", e);
        }
    }

    private void doWork() {
        log.info("starting work");
        Iterable<ChannelConfig> channels = channelConfigDao.getAll(false);
        S3ConfigLockable lockable = new S3ConfigLockable(channels);
        distributedLockRunner.setLockPath("/S3ConfigLock");
        distributedLockRunner.runWithLock(lockable, 1, TimeUnit.MINUTES);
    }

    private class S3ConfigInit extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            Random random = new Random();
            long minutes = TimeUnit.HOURS.toMinutes(6);
            long delayMinutes = minutes + (long) random.nextInt((int) minutes);
            log.info("scheduling S3Config with delay " + delayMinutes);
            return Scheduler.newFixedDelaySchedule(0, delayMinutes, TimeUnit.MINUTES);
        }

    }

    private class S3ConfigLockable implements Lockable {
        final Iterable<ChannelConfig> configurations;

        private S3ConfigLockable(Iterable<ChannelConfig> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void takeLeadership(Leadership leadership) throws Exception {
            updateTtlDays();
            updateMaxItems();
        }

        private void updateMaxItems() {
            log.info("updating max items");
            for (ChannelConfig config : configurations) {
                if (config.getMaxItems() > 0 && !config.getKeepForever()) {
                    updateMaxItems(config);
                }
            }
        }

        private void updateMaxItems(ChannelConfig config) {
            String name = config.getDisplayName();
            log.info("updating max items for channel {}", name);
            ActiveTraces.start("S3Config.updateMaxItems", name);
            Optional<ContentKey> optional = channelService.getLatest(name, false);
            if (optional.isPresent()) {
                ContentKey latest = optional.get();
                if (latest.getTime().isAfter(TimeUtil.now().minusDays(1))) {
                    updateMaxItems(config, latest);
                }
            }
            ActiveTraces.end();
            log.info("completed max items for channel {}", name);

        }

        private void updateMaxItems(ChannelConfig config, ContentKey latest) {
            SortedSet<ContentKey> keys = new TreeSet<>();
            keys.add(latest);
            String name = config.getDisplayName();
            DirectionQuery query = DirectionQuery.builder()
                    .channelName(name)
                    .startKey(latest)
                    .next(false)
                    .stable(false)
                    .earliestTime(config.getTtlTime())
                    .count((int) (config.getMaxItems() - 1))
                    .build();
            keys.addAll(channelService.query(query));
            if (keys.size() == config.getMaxItems()) {
                ContentKey limitKey = keys.first();
                log.info("deleting keys before {}", limitKey);
                channelService.deleteBefore(name, limitKey);
            }
        }

        private void updateTtlDays() {
            log.info("updateTtlDays");
            ActiveTraces.start("S3Config.updateTtlDays");
            int maxRules = HubProperties.getProperty("s3.maxRules", S3_LIFECYCLE_RULES_AVAILABLE);
            List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();

            if (maxRules > 0 && maxRules <= S3_LIFECYCLE_RULES_AVAILABLE) {
                rules = S3ConfigStrategy.apportion(configurations, new DateTime(), maxRules);
            }

            log.info("updating {} rules with ttl life cycle ", rules.size());
            log.trace("updating {} ", rules);

            if (!rules.isEmpty()) {
                BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(rules);
                SetBucketLifecycleConfigurationRequest request = new SetBucketLifecycleConfigurationRequest(s3BucketName, lifecycleConfig);
                s3Client.setBucketLifecycleConfiguration(request);
            }
            ActiveTraces.end();
        }
    }

}
