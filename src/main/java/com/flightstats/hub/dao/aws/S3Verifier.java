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

    SortedSet<ContentKey> getMissing(DateTime startTime, DateTime endTime, String channelName, ContentDao s3ContentDao,
                                     SortedSet<ContentKey> foundCacheKeys) {
        SortedSet<ContentKey> cacheKeys = new TreeSet<>();
        SortedSet<ContentKey> longTermKeys = new TreeSet<>();
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .endTime(endTime)
                .unit(TimeUtil.Unit.MINUTES)
                .build();
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

    private void singleS3Verification(final DateTime startTime, final ChannelConfig channel, DateTime endTime) {
        channelThreadPool.submit(() -> {
            try {
                Thread.currentThread().setName("s3-single-" + channel + "-" + TimeUtil.minutes(startTime));
                ActiveTraces.start("S3WriterManager.singleS3Verification", channel, startTime);
                String channelName = channel.getName();
                SortedSet<ContentKey> keysToAdd = getMissing(startTime, endTime, channelName, s3SingleContentDao, new TreeSet<>());
                for (ContentKey key : keysToAdd) {
                    s3WriteQueue.add(new ChannelContentKey(channelName, key));
                }
            } finally {
                ActiveTraces.end();
            }
        });
    }

    private void batchS3Verification(VerifierRange verifierRange) {
        channelThreadPool.submit(() -> {
            try {
                ChannelConfig channel = verifierRange.channel;
                MinutePath currentPath = verifierRange.lastUpdated;
                MinutePath endPath = verifierRange.endPath;

                Thread.currentThread().setName("s3-batch-" + channel + "-" + currentPath.toUrl());
                ActiveTraces.start("S3WriterManager.batchS3Verification", channel, currentPath, endPath);
                String channelName = channel.getName();
                while ((currentPath.compareTo(endPath) <= 0)) {
                    SortedSet<ContentKey> cacheKeys = new TreeSet<>();
                    ActiveTraces.getLocal().add("S3WriterManager.path", channel, currentPath);
                    SortedSet<ContentKey> keysToAdd = getMissing(currentPath.getTime(), null, channelName, s3BatchContentDao, cacheKeys);
                    if (!keysToAdd.isEmpty()) {
                        logger.info("batchS3Verification {} missing {}", channelName, currentPath);
                        String batchUrl = TimedGroupStrategy.getBulkUrl(APP_URL + "channel/" + channelName, currentPath, "batch");
                        logger.debug("batchS3Verification batchUrl {}", batchUrl);
                        S3BatchResource.getAndWriteBatch(s3BatchContentDao, channelName, currentPath, cacheKeys, batchUrl);
                    }
                    lastContentPath.get(channel.getName(), currentPath, LAST_BATCH_VERIFIED);
                    ActiveTraces.getLocal().add("S3WriterManager.updated", channel, currentPath);
                    currentPath = currentPath.addMinute();
                }

            } finally {
                ActiveTraces.end();
            }
        });
    }

    public void runSingle() {
        try {
            DateTime endTime = DateTime.now();
            DateTime startTime = endTime.minusMinutes(offsetMinutes).minusMinutes(1);
            logger.info("Verifying Single S3 data at: {}", startTime);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isSingle() || channel.isBoth()) {
                    singleS3Verification(startTime, channel, endTime);
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    public void runBatch() {
        try {
            DateTime now = TimeUtil.now();
            logger.info("Verifying Batch S3 data from: {}", now);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isBatch() || channel.isBoth()) {
                    batchS3Verification(getVerifierRange(now, channel));
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    VerifierRange getVerifierRange(DateTime now, ChannelConfig channel) {
        VerifierRange verifierRange = new VerifierRange();
        MinutePath spokeTtlTime = getSpokeTtlPath(now);
        verifierRange.endPath = new MinutePath(now.minusMinutes(offsetMinutes));
        if (channel.isReplicating()) {
            ContentPath contentPath = lastContentPath.get(channel.getName(), new MinutePath(now), ChannelReplicator.REPLICATED_LAST_UPDATED);
            verifierRange.endPath = new MinutePath(contentPath.getTime().minusMinutes(offsetMinutes));
        }
        verifierRange.lastUpdated = (MinutePath) lastContentPath.get(channel.getName(), verifierRange.endPath, LAST_BATCH_VERIFIED);
        if (verifierRange.lastUpdated.compareTo(spokeTtlTime) < 0) {
            verifierRange.lastUpdated = spokeTtlTime;
        } else if (verifierRange.lastUpdated.compareTo(verifierRange.endPath) < 0) {
            verifierRange.lastUpdated = verifierRange.lastUpdated.addMinute();
        }
        return verifierRange;
    }

    class VerifierRange {
        MinutePath lastUpdated;
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
