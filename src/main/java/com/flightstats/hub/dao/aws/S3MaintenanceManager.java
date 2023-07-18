package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.GetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.DistributedAsyncLockRunner;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.cluster.Lockable;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.name.Named;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;



@Slf4j
public class S3MaintenanceManager {
    // S3 limits max lifecycle rules to 1000. 10 rules are made available for setting lifecycle rules from infrastructure(terraform) code.
    public static final Integer S3_LIFECYCLE_RULES_AVAILABLE = 990;
    private final DistributedAsyncLockRunner distributedLockRunner;
    private final Dao<ChannelConfig> channelConfigDao;
    private final MaxItemsEnforcer maxItemsEnforcer;
    private final HubS3Client s3Client;

    private final String bucketName;

    private final int maxRules;

    private final AtomicReference<HubS3LifecycleRequest> lastLifecycleApplied = new AtomicReference<>();

    @Inject
    public S3MaintenanceManager(HubS3Client s3Client,
                                DistributedAsyncLockRunner distributedLockRunner,
                                @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                                MaxItemsEnforcer maxItemsEnforcer,
                                String bucketName,
                                int maxRules,
                                boolean enabled) {
        this.s3Client = s3Client;
        this.distributedLockRunner = distributedLockRunner;
        this.channelConfigDao = channelConfigDao;
        this.maxItemsEnforcer = maxItemsEnforcer;
        this.bucketName = bucketName;
        this.maxRules = maxRules;

        if (enabled) {
            HubServices.register(new S3MaintenanceManagerInit());
        }
    }

    public HubS3LifecycleRequest getLastLifecycleApplied() {
        return lastLifecycleApplied.get();
    }

    private void run() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("unable to update config", e);
        }
    }

    private void doWork() {
        log.debug("starting work");
        Iterable<ChannelConfig> channels = channelConfigDao.getAll(false);
        S3MaintenanceManagerLock lockable = new S3MaintenanceManagerLock(channels);
        distributedLockRunner.setLockPath("/S3ConfigLock");
        distributedLockRunner.runWithLock(lockable, 1, TimeUnit.MINUTES);
    }

    private class S3MaintenanceManagerInit extends AbstractScheduledService {
        @Override
        protected void runOneIteration() {
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

    private class S3MaintenanceManagerLock implements Lockable {
        final Iterable<ChannelConfig> configurations;

        private S3MaintenanceManagerLock(Iterable<ChannelConfig> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void takeLeadership(Leadership leadership) {
            updateTtlDays();
            maxItemsEnforcer.updateMaxItems(configurations);
        }

        private void updateRulesConfigForBucket(List<BucketLifecycleConfiguration.Rule> rules) {
            List<BucketLifecycleConfiguration.Rule> withNonHubRules = Stream.of(rules, getNonHubBucketLifecycleRules(bucketName))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(withNonHubRules);
            SetBucketLifecycleConfigurationRequest request =
                    new SetBucketLifecycleConfigurationRequest(bucketName, lifecycleConfig);
            s3Client.setBucketLifecycleConfiguration(request);

            log.info("updated {} rules with ttl life cycle for s3 bucket: {}", rules.size(), bucketName);
            lastLifecycleApplied.set(new HubS3LifecycleRequest(DateTime.now(), request));
        }

        private void updateTtlDays() {
            log.debug("updateTtlDays");
            ActiveTraces.start("S3Config.updateTtlDays");
            if (maxRules > 0 && maxRules <= S3_LIFECYCLE_RULES_AVAILABLE) {
                List<BucketLifecycleConfiguration.Rule> rules = S3ConfigStrategy
                        .apportion(configurations, new DateTime(), maxRules);
                log.trace("updating {} ", rules);
                if (!rules.isEmpty()) {
                    this.updateRulesConfigForBucket(rules);
                }
            }
            ActiveTraces.end();
        }

        private List<BucketLifecycleConfiguration.Rule> getNonHubBucketLifecycleRules(String bucketName) {
            GetBucketLifecycleConfigurationRequest request =
                    new GetBucketLifecycleConfigurationRequest(bucketName);
            BucketLifecycleConfiguration bucketLifecycleConfiguration =
                    s3Client.getBucketLifecycleConfiguration(request);
            return S3ConfigStrategy.getNonHubBucketLifecycleRules(bucketLifecycleConfiguration);

        }
    }

    @Value
    public static class HubS3LifecycleRequest {
        DateTime requestTime;
        SetBucketLifecycleConfigurationRequest request;
    }
}
