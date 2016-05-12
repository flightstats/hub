package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.group.TimedGroupStrategy;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class S3Verifier {
    private final static Logger logger = LoggerFactory.getLogger(S3Verifier.class);
    private static final String APP_URL = HubProperties.getAppUrl();
    static final String LAST_BATCH_VERIFIED = "/S3VerifierBatchLastVerified";
    static final String LAST_SINGLE_VERIFIED = "/S3VerifierSingleLastVerified";

    @Inject
    private ChannelService channelService;
    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;
    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;
    @Inject
    private S3WriteQueue s3WriteQueue;
    @Inject
    LastContentPath lastContentPath;

    private final int offsetMinutes = HubProperties.getProperty("s3Verifier.offsetMinutes", 15);
    private final double keepLeadershipRate = HubProperties.getProperty("s3Verifier.keepLeadershipRate", 0.75);
    private final ExecutorService queryThreadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("S3QueryThread-%d").build());
    private final ExecutorService channelThreadPool = Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("S3ChannelThread-%d").build());

    public S3Verifier() {
        if (HubProperties.getProperty("s3Verifier.run", true)) {
            registerService(new S3VerifierService("/S3VerifierSingleService", offsetMinutes, this::runSingle));
            registerService(new S3VerifierService("/S3VerifierBatchService", 1, this::runBatch));
        }
    }

    private void registerService(S3VerifierService service) {
        HubServices.register(service, HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
    }

    SortedSet<ContentKey> getMissing(MinutePath startPath, MinutePath endPath, String channelName, ContentDao s3ContentDao,
                                     SortedSet<ContentKey> foundCacheKeys) {
        SortedSet<ContentKey> cacheKeys = new TreeSet<>();
        SortedSet<ContentKey> longTermKeys = new TreeSet<>();
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES);
        if (endPath != null) {
            builder.endTime(endPath.getTime());
        }
        TimeQuery timeQuery = builder.build();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            Traces traces = ActiveTraces.getLocal();
            queryThreadPool.submit(() -> {
                ActiveTraces.setLocal(traces);
                SortedSet<ContentKey> spokeKeys = spokeContentDao.queryByTime(timeQuery);
                cacheKeys.addAll(spokeKeys);
                foundCacheKeys.addAll(spokeKeys);
                countDownLatch.countDown();
            });
            queryThreadPool.submit(() -> {
                ActiveTraces.setLocal(traces);
                longTermKeys.addAll(s3ContentDao.queryByTime(timeQuery));
                countDownLatch.countDown();
            });
            countDownLatch.await(15, TimeUnit.MINUTES);
            cacheKeys.removeAll(longTermKeys);
            if (cacheKeys.size() > 0) {
                logger.info("missing items {} {}", channelName, cacheKeys);
            }
            return cacheKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private void singleS3Verification(VerifierRange range) {
        channelThreadPool.submit(() -> {
            try {
                Thread.currentThread().setName("s3-single-" + range.channel + "-" + range.startPath.toUrl());
                ActiveTraces.start("S3Verifier.singleS3Verification", range);
                String channelName = range.channel.getName();
                logger.debug("singleS3Verification {}", range);
                SortedSet<ContentKey> keysToAdd = getMissing(range.startPath, range.endPath, channelName, s3SingleContentDao, new TreeSet<>());
                for (ContentKey key : keysToAdd) {
                    logger.trace("found missing {} {}", channelName, key);
                    s3WriteQueue.add(new ChannelContentKey(channelName, key));
                }
                logger.debug("singleS3Verification.completed {}", range);
                lastContentPath.updateIncrease(range.endPath, range.channel.getName(), LAST_SINGLE_VERIFIED);
            } finally {
                ActiveTraces.end();
            }
        });
    }

    private void batchS3Verification(VerifierRange range) {
        channelThreadPool.submit(() -> {
            try {
                MinutePath currentPath = range.startPath;
                String channelName = range.channel.getName();
                Thread.currentThread().setName("s3-batch-" + channelName + "-" + currentPath.toUrl());
                ActiveTraces.start("S3Verifier.batchS3Verification", range);
                logger.debug("batchS3Verification {}", range);
                while ((currentPath.compareTo(range.endPath) <= 0)) {
                    logger.debug("batch minute {}", currentPath, channelName);
                    SortedSet<ContentKey> cacheKeys = new TreeSet<>();
                    ActiveTraces.getLocal().add("S3Verifier.path", channelName, currentPath);
                    SortedSet<ContentKey> keysToAdd = getMissing(currentPath, null, channelName, s3BatchContentDao, cacheKeys);
                    if (!keysToAdd.isEmpty()) {
                        logger.info("batchS3Verification {} missing {}", channelName, currentPath);
                        String batchUrl = TimedGroupStrategy.getBulkUrl(APP_URL + "channel/" + channelName, currentPath, "batch");
                        S3BatchResource.getAndWriteBatch(s3BatchContentDao, channelName, currentPath, cacheKeys, batchUrl);
                    }
                    lastContentPath.updateIncrease(currentPath, range.channel.getName(), LAST_BATCH_VERIFIED);
                    ActiveTraces.getLocal().add("S3Verifier.updated", currentPath);
                    currentPath = currentPath.addMinute();
                }

            } catch (Exception e) {
                logger.error("S3Verifier.batchS3Verification Error: ", e);
            } finally {
                ActiveTraces.end();
            }
        });
    }

    public void runSingle() {
        try {
            DateTime now = TimeUtil.now();
            logger.info("Verifying Single S3 data at: {}", now);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isSingle() || channel.isBoth()) {
                    singleS3Verification(getSingleVerifierRange(now, channel));
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    VerifierRange getSingleVerifierRange(DateTime now, ChannelConfig channel) {
        VerifierRange range = new VerifierRange(channel);
        MinutePath spokeTtlTime = getSpokeTtlPath(now);
        if (channel.isReplicating()) {
            ContentPath contentPath = lastContentPath.get(channel.getName(), new MinutePath(now), ChannelReplicator.REPLICATED_LAST_UPDATED);
            now = contentPath.getTime();
        }
        DateTime start = now.minusMinutes(1);
        range.endPath = new MinutePath(start);
        MinutePath defaultStart = new MinutePath(start.minusMinutes(offsetMinutes));
        range.startPath = (MinutePath) lastContentPath.get(channel.getName(), defaultStart, LAST_SINGLE_VERIFIED);
        if (!channel.isReplicating() && range.startPath.compareTo(spokeTtlTime) < 0) {
            range.startPath = spokeTtlTime;
        }
        return range;
    }

    public void runBatch() {
        try {
            DateTime now = TimeUtil.now();
            logger.info("Verifying Batch S3 data from: {}", now);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isBatch() || channel.isBoth()) {
                    batchS3Verification(getBatchVerifierRange(now, channel));
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    VerifierRange getBatchVerifierRange(DateTime now, ChannelConfig channel) {
        VerifierRange range = new VerifierRange(channel);
        MinutePath spokeTtlTime = getSpokeTtlPath(now);
        if (channel.isReplicating()) {
            ContentPath contentPath = lastContentPath.get(channel.getName(), new MinutePath(now), ChannelReplicator.REPLICATED_LAST_UPDATED);
            now = contentPath.getTime();
        }
        range.endPath = new MinutePath(now.minusMinutes(offsetMinutes));
        range.startPath = (MinutePath) lastContentPath.get(channel.getName(), range.endPath, LAST_BATCH_VERIFIED);
        if (!channel.isReplicating() && range.startPath.compareTo(spokeTtlTime) < 0) {
            range.startPath = spokeTtlTime;
        } else if (range.startPath.compareTo(range.endPath) < 0) {
            range.startPath = range.startPath.addMinute();
        }
        return range;
    }

    @ToString
    class VerifierRange {

        public VerifierRange(ChannelConfig channel) {
            this.channel = channel;
        }

        MinutePath startPath;
        MinutePath endPath;
        ChannelConfig channel;
    }

    private MinutePath getSpokeTtlPath(DateTime now) {
        return new MinutePath(now.minusMinutes(HubProperties.getSpokeTtl() - 2));
    }

    @AllArgsConstructor
    private class S3VerifierService extends AbstractIdleService implements Leader {

        private String leaderPath;
        private int minutes;
        private Runnable runnable;

        @Override
        protected void startUp() throws Exception {
            CuratorLeader curatorLeader = new CuratorLeader(leaderPath, this);
            curatorLeader.start();
        }

        @Override
        protected void shutDown() throws Exception {
            s3WriteQueue.close();
        }

        @Override
        public double keepLeadershipRate() {
            return keepLeadershipRate;
        }

        @Override
        public void takeLeadership(AtomicBoolean hasLeadership) {
            while (hasLeadership.get()) {
                long start = System.currentTimeMillis();
                runnable.run();
                long sleep = TimeUnit.MINUTES.toMillis(minutes) - (System.currentTimeMillis() - start);
                Sleeper.sleep(Math.max(0, sleep));
            }

        }
    }
}
