package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelConfigDao;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class S3Config {
    private final static Logger logger = LoggerFactory.getLogger(S3Config.class);

    private final AmazonS3 s3Client;
    private final CuratorLock curatorLock;
    private final ChannelConfigDao channelConfigDao;
    private final String s3BucketName;

    @Inject
    public S3Config(AmazonS3 s3Client, S3BucketName s3BucketName,
                    CuratorLock curatorLock, ChannelConfigDao channelConfigDao) {
        this.s3Client = s3Client;
        this.curatorLock = curatorLock;
        this.channelConfigDao = channelConfigDao;
        this.s3BucketName = s3BucketName.getS3BucketName();
        HubServices.register(new S3ConfigInit());
    }

    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            logger.warn("unable to update config", e);
        }
    }

    public int doWork() {
        logger.info("starting work");
        Iterable<ChannelConfig> channels = channelConfigDao.getChannels();
        S3ConfigLockable lockable = new S3ConfigLockable(channels);
        curatorLock.runWithLock(lockable, "/S3ConfigLock/", 1, TimeUnit.MINUTES);
        return lockable.size;
    }

    private class S3ConfigInit extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            long delayMinutes = TimeUnit.DAYS.toMinutes(1);
            Random random = new Random();
            long offsetMinutes = random.nextInt((int) (delayMinutes / 2)) + TimeUnit.HOURS.toMinutes(1);
            logger.info("scheduling S3Config with offsetMinutes=" + offsetMinutes);
            return Scheduler.newFixedDelaySchedule(offsetMinutes, delayMinutes, TimeUnit.MINUTES);
        }

    }

    private class S3ConfigLockable implements Lockable {
        final Iterable<ChannelConfig> configurations;
        private int size;

        private S3ConfigLockable(Iterable<ChannelConfig> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void runWithLock() throws Exception {
            logger.info("running with lock");
            ArrayList<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
            for (ChannelConfig config : configurations) {
                String namePrefix = config.getName() + "/";
                BucketLifecycleConfiguration.Rule configRule = new BucketLifecycleConfiguration.Rule()
                        .withPrefix(namePrefix)
                        .withId(config.getName())
                        .withExpirationInDays((int) config.getTtlDays())
                        .withStatus(BucketLifecycleConfiguration.ENABLED);
                rules.add(configRule);
            }
            logger.info("updating " + rules.size());
            BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(rules);
            s3Client.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);
            size = rules.size();
        }
    }

}
