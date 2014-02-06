package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelConfigurationDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.util.Started;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * We want to update S3Configurations asynchronously, primarily to prevent collisions during integration tests,
 * which could also happen in the real world.  This also speeds up Sequence channel creation.
 */
public class S3Config implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(S3Config.class);

    private static final Started started = new Started();
    private final AmazonS3 s3Client;
    private final CuratorLock curatorLock;
    private final ChannelConfigurationDao channelConfigurationDao;
    private final String s3BucketName;

    @Inject
    public S3Config(AmazonS3 s3Client, @Named("app.environment") String environment, @Named("app.name") String appName,
                    CuratorLock curatorLock, ChannelConfigurationDao channelConfigurationDao) {
        this.s3Client = s3Client;
        this.curatorLock = curatorLock;
        this.channelConfigurationDao = channelConfigurationDao;
        this.s3BucketName = appName + "-" + environment;
    }

    public void initialize() {
        if (started.start()) {
            return;
        }
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long delayMinutes = TimeUnit.DAYS.toMinutes(1);
        Random random = new Random();
        long offsetMinutes = random.nextInt((int) (delayMinutes / 2)) + TimeUnit.HOURS.toMinutes(1);
        logger.info("scheduling S3Config with offsetMinutes=" + offsetMinutes);
        executor.scheduleWithFixedDelay(this, offsetMinutes, delayMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            logger.warn("unable to update config", e);
        }
    }

    public int doWork() {
        Iterable<ChannelConfiguration> channels = channelConfigurationDao.getChannels();
        S3ConfigLockable lockable = new S3ConfigLockable(channels);
        curatorLock.runWithLock(lockable, "/S3ConfigLock/", 1, TimeUnit.SECONDS);
        return lockable.size;
    }

    private class S3ConfigLockable implements Lockable {
        final Iterable<ChannelConfiguration> configurations;
        private int size;

        private S3ConfigLockable(Iterable<ChannelConfiguration> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void runWithLock() throws Exception {
            ArrayList<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
            for (ChannelConfiguration config : configurations) {
                if (config.isSequence()) {
                    String namePrefix = config.getName() + "/";
                    BucketLifecycleConfiguration.Rule configRule = new BucketLifecycleConfiguration.Rule()
                            .withPrefix(namePrefix)
                            .withId(config.getName())
                            .withExpirationInDays((int) config.getTtlDays())
                            .withStatus(BucketLifecycleConfiguration.ENABLED);
                    rules.add(configRule);
                }
            }

            BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(rules);
            s3Client.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);
            size = rules.size();
            logger.info("updated " + size);
        }
    }

    public boolean isStarted() {
        return started.isStarted();
    }
}
