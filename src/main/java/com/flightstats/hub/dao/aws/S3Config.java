package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.GetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.DistributedAsyncLockRunner;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
public class S3Config {

    // S3 limits max lifecycle rules to 1000. 10 rules are made available for setting lifecycle rules from infrastructure(terraform) code.
    private static final Integer S3_LIFECYCLE_RULES_AVAILABLE = 990;

    private final DistributedAsyncLockRunner distributedLockRunner;
    private final Dao<ChannelConfig> channelConfigDao;
    private final MaxItemsEnforcer maxItemsEnforcer;
    private final HubS3Client s3Client;
    private final S3Properties s3Properties;

    @Inject
    public S3Config(HubS3Client s3Client,
                    DistributedAsyncLockRunner distributedLockRunner,
                    @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                    MaxItemsEnforcer maxItemsEnforcer,
                    S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.distributedLockRunner = distributedLockRunner;
        this.channelConfigDao = channelConfigDao;
        this.maxItemsEnforcer = maxItemsEnforcer;
        this.s3Properties = s3Properties;
        if (s3Properties.isConfigManagementEnabled()) {
            HubServices.register(new S3ConfigInit());
        }
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
        S3ConfigLockable lockable = new S3ConfigLockable(channels);
        distributedLockRunner.setLockPath("/S3ConfigLock");
        distributedLockRunner.runWithLock(lockable, 1, TimeUnit.MINUTES);
    }

    private class S3ConfigInit extends AbstractScheduledService {
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

    private class S3ConfigLockable implements Lockable {
        final Iterable<ChannelConfig> configurations;

            private S3ConfigLockable(Iterable<ChannelConfig> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void takeLeadership(Leadership leadership) {
            Stream.of(s3Properties.getBucketName(), s3Properties.getDisasterRecoveryBucketName())
                    .forEach(bucketName -> {
                        updateTtlDays(bucketName);
                        maxItemsEnforcer.updateMaxItems(configurations, bucketName);
                    });
        }

        private void updateTtlDays(String bucketName) {
            log.debug("updateTtlDays");
            ActiveTraces.start("S3Config.updateTtlDays");
            int maxRules = s3Properties.getBucketPolicyMaxRules(S3_LIFECYCLE_RULES_AVAILABLE);

            List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();

            if (maxRules > 0 && maxRules <= S3_LIFECYCLE_RULES_AVAILABLE) {
                rules = S3ConfigStrategy.apportion(configurations, new DateTime(), maxRules);
            }

            log.trace("updating {} ", rules);

            if (!rules.isEmpty()) {
                rules.addAll(getNonHubBucketLifecycleRules(bucketName));

                BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(rules);
                SetBucketLifecycleConfigurationRequest request =
                        new SetBucketLifecycleConfigurationRequest(bucketName, lifecycleConfig);
                s3Client.setBucketLifecycleConfiguration(request);
                log.info("updated {} rules with ttl life cycle ", rules.size());

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

}
