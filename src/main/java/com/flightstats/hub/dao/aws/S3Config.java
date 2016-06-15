package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelConfigDao;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class S3Config {
    private final static Logger logger = LoggerFactory.getLogger(S3Config.class);

    private final AmazonS3 s3Client;
    private final CuratorLock curatorLock;
    private final ChannelConfigDao channelConfigDao;
    private final String s3BucketName;
    private ChannelService channelService;

    @Inject
    public S3Config(AmazonS3 s3Client, S3BucketName s3BucketName,
                    CuratorLock curatorLock, ChannelConfigDao channelConfigDao, ChannelService channelService) {
        this.s3Client = s3Client;
        this.curatorLock = curatorLock;
        this.channelConfigDao = channelConfigDao;
        this.channelService = channelService;
        this.s3BucketName = s3BucketName.getS3BucketName();
        HubServices.register(new S3ConfigInit());
    }

    private void run() {
        try {
            doWork();
        } catch (Exception e) {
            logger.warn("unable to update config", e);
        }
    }

    private void doWork() {
        logger.info("starting work");
        Iterable<ChannelConfig> channels = channelConfigDao.getChannels(false);
        S3ConfigLockable lockable = new S3ConfigLockable(channels);
        curatorLock.runWithLock(lockable, "/S3ConfigLock", 1, TimeUnit.MINUTES);
    }

    private BucketLifecycleConfiguration.Rule addRule(ChannelConfig config, String postfix) {
        String id = config.getName() + postfix;
        return new BucketLifecycleConfiguration.Rule()
                .withPrefix(id + "/")
                .withId(id)
                .withExpirationInDays((int) config.getTtlDays())
                .withStatus(BucketLifecycleConfiguration.ENABLED);
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
            logger.info("scheduling S3Config with delay " + delayMinutes);
            return Scheduler.newFixedDelaySchedule(0, delayMinutes, TimeUnit.MINUTES);
        }

    }

    private class S3ConfigLockable implements Lockable {
        final Iterable<ChannelConfig> configurations;

        private S3ConfigLockable(Iterable<ChannelConfig> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void runWithLock() throws Exception {
            updateTtlDays();
            updateMaxItems();
        }

        private void updateMaxItems() {
            logger.info("updating max items");
            for (ChannelConfig config : configurations) {
                if (config.getMaxItems() > 0) {
                    updateMaxItems(config);

                }
            }
        }

        private void updateMaxItems(ChannelConfig config) {
            logger.info("updating max items for channel {}", config.getName());
            ActiveTraces.start("S3Config.updateMaxItems", config.getName());
            Optional<ContentKey> optional = channelService.getLatest(config.getName(), false, false);
            if (optional.isPresent()) {
                ContentKey latest = optional.get();
                if (latest.getTime().isAfter(TimeUtil.now().minusDays(1))) {
                    updateMaxItems(config, latest);
                }
            }
            ActiveTraces.end();
            logger.info("completed max items for channel {}", config.getName());

        }

        private void updateMaxItems(ChannelConfig config, ContentKey latest) {
            SortedSet<ContentKey> keys = new TreeSet<>();
            keys.add(latest);
            DirectionQuery query = DirectionQuery.builder()
                    .channelName(config.getName())
                    .contentKey(latest)
                    .next(false)
                    .stable(false)
                    .ttlDays(0)
                    .count((int) (config.getMaxItems() - 1))
                    .build();
            keys.addAll(channelService.getKeys(query));
            if (keys.size() == config.getMaxItems()) {
                ContentKey limitKey = keys.first();
                logger.info("deleting keys before {}", limitKey);
                channelService.deleteBefore(config.getName(), limitKey);
            }
        }

        private void updateTtlDays() {
            logger.info("updateTtlDays");
            ActiveTraces.start("S3Config.updateTtlDays");
            ArrayList<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
            for (ChannelConfig config : configurations) {
                if (config.getTtlDays() > 0) {
                    if (config.isSingle() || config.isBoth()) {
                        rules.add(addRule(config, ""));
                    }
                    if (config.isBatch() || config.isBoth()) {
                        rules.add(addRule(config, "Batch"));
                    }
                }
            }
            logger.info("updating " + rules.size() + " rules with ttl life cycle ");
            if (!rules.isEmpty()) {
                BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(rules);
                s3Client.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);
            }
            ActiveTraces.end();
        }
    }

}
