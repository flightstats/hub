package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierMetrics;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public class S3Verifier {
    static final String LAST_SINGLE_VERIFIED = "/S3VerifierSingleLastVerified/";
    private final static Logger logger = LoggerFactory.getLogger(S3Verifier.class);
    private static final String LEADER_PATH = "/S3VerifierSingleService";

    private final long baseTimeoutMinutes;
    private final int offsetMinutes = HubProperties.getProperty("s3Verifier.offsetMinutes", 15);
    private final int channelThreads = HubProperties.getProperty("s3Verifier.channelThreads", 3);
    private final ExecutorService channelThreadPool = Executors.newFixedThreadPool(channelThreads, new ThreadFactoryBuilder().setNameFormat("S3VerifierChannel-%d").build());
    private final ExecutorService queryThreadPool = Executors.newFixedThreadPool(channelThreads * 2, new ThreadFactoryBuilder().setNameFormat("S3VerifierQuery-%d").build());

    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private final ContentDao spokeWriteContentDao;
    private final ContentDao s3SingleContentDao;
    private final S3WriteQueue s3WriteQueue;
    private final Client httpClient;
    private final DistributedLeadershipLockManager distributedLeadershipLockManager;
    private final MetricsService metricsService;

    @Inject
    public S3Verifier(LastContentPath lastContentPath,
                      ChannelService channelService,
                      @Named(ContentDao.WRITE_CACHE) ContentDao spokeWriteContentDao,
                      @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
                      S3WriteQueue s3WriteQueue,
                      Client httpClient,
                      DistributedLeadershipLockManager distributedLeadershipLockManager,
                      MetricsService metricsService) {
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.s3WriteQueue = s3WriteQueue;
        this.httpClient = httpClient;
        this.distributedLeadershipLockManager = distributedLeadershipLockManager;
        this.metricsService = metricsService;
        this.baseTimeoutMinutes = HubProperties.getProperty("s3Verifier.baseTimeoutMinutes", 2);

        if (HubProperties.getProperty("s3Verifier.run", true)) {
            HubServices.register(new S3ScheduledVerifierService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        }
    }

    private void verifySingleChannels() {
        try {
            logger.info("Verifying Single S3 data");
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isSingle() || channel.isBoth()) {
                    channelThreadPool.submit(() -> {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName(name + "|" + channel.getDisplayName());
                        String url = HubProperties.getAppUrl() + "internal/s3Verifier/" + channel.getDisplayName();
                        logger.debug("calling {}", url);
                        ClientResponse post = null;
                        try {
                            post = httpClient.resource(url).post(ClientResponse.class);
                            logger.debug("response from post {}", post);
                        } finally {
                            HubUtils.close(post);
                            Thread.currentThread().setName(name);
                        }
                    });
                }
            }
            logger.info("Completed Verifying Single S3 data");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    void verifyChannel(String channelName) {
        DateTime now = TimeUtil.now();
        ChannelConfig channel = channelService.getChannelConfig(channelName, false);
        if (channel == null) {
            return;
        }
        VerifierRange range = getSingleVerifierRange(now, channel);
        if (range != null) {
            verifyChannel(range);
        }
    }

    VerifierRange getSingleVerifierRange(DateTime now, ChannelConfig channelConfig) {
        MinutePath spokeTtlTime = getSpokeTtlPath(now);
        now = channelService.getLastUpdated(channelConfig.getDisplayName(), new MinutePath(now)).getTime();
        DateTime start = now.minusMinutes(1);
        MinutePath endPath = new MinutePath(start);
        MinutePath defaultStart = new MinutePath(start.minusMinutes(offsetMinutes));
        MinutePath startPath = (MinutePath) lastContentPath.get(channelConfig.getDisplayName(), defaultStart, LAST_SINGLE_VERIFIED);
        if (channelConfig.isLive() && startPath.compareTo(spokeTtlTime) < 0) {
            startPath = spokeTtlTime;
        }
        return VerifierRange.builder()
                .channelConfig(channelConfig)
                .startPath(startPath)
                .endPath(endPath)
                .build();
    }

    @VisibleForTesting
    void verifyChannel(VerifierRange range) {
        String channelName = range.getChannelConfig().getDisplayName();
        SortedSet<ContentKey> keysToAdd = getMissing(range.getStartPath(), range.getEndPath(), channelName, s3SingleContentDao, new TreeSet<>());
        logger.debug("verifyChannel.starting {}", range);
        MinutePath lastCompleted = range.getEndPath();
        for (ContentKey key : keysToAdd) {
            logger.trace("found missing {} {}", channelName, key);
            incrementMetric(VerifierMetrics.MISSING_ITEM);
            boolean success = s3WriteQueue.add(new ChannelContentKey(channelName, key));
            if (!success) {
                logger.error("unable to queue missing item {} {}", channelName, key);
                incrementMetric(VerifierMetrics.FAILED);
                lastCompleted = new MinutePath(key.getTime().minusMinutes(1));
                break;
            }
        }

        if (lastCompleted.compareTo(range.getStartPath()) > 0) {
            logger.debug("verifyChannel.completed {}", range);
            lastContentPath.updateIncrease(lastCompleted, range.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
            incrementMetric(VerifierMetrics.PARTIAL_UPDATE);
        } else {
            logger.warn("verifyChannel completed, but start time is the same as last completed");
            incrementMetric(VerifierMetrics.EXCESSIVE_CHANNEL_VOLUME);
        }
    }

    @VisibleForTesting
    SortedSet<ContentKey> getMissing(MinutePath startPath, MinutePath endPath, String channelName, ContentDao s3ContentDao,
                                             SortedSet<ContentKey> foundCacheKeys) {
        long timeout = baseTimeoutMinutes;
        QueryResult queryResult = new QueryResult(1);
        SortedSet<ContentKey> longTermKeys = new TreeSet<>();
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES);
        if (endPath != null) {
            Duration duration = new Duration(startPath.getTime(), endPath.getTime());
            timeout += duration.getStandardDays();
            builder.limitKey(ContentKey.lastKey(endPath.getTime()));
        }
        TimeQuery timeQuery = builder.build();
        try {
            CountDownLatch latch = new CountDownLatch(2);
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> {
                SortedSet<ContentKey> spokeKeys = spokeWriteContentDao.queryByTime(timeQuery);
                foundCacheKeys.addAll(spokeKeys);
                queryResult.addKeys(spokeKeys);
            });
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> longTermKeys.addAll(s3ContentDao.queryByTime(timeQuery)));
            latch.await(timeout, TimeUnit.MINUTES);
            if (latch.getCount() != 0) {
                logger.error("s3 verifier timed out while finding missing items, write queue is backing up");
                incrementMetric(VerifierMetrics.TIMEOUT);
            }
            queryResult.getContentKeys().removeAll(longTermKeys);
            if (queryResult.getContentKeys().size() > 0) {
                logger.info("missing items {} {}", channelName, queryResult.getContentKeys());
            }
            if (queryResult.hadSuccess()) {
                return queryResult.getContentKeys();
            }
            throw new FailedQueryException("unable to query spoke");
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private void runInQueryPool(Traces traces, CountDownLatch countDownLatch, Runnable runnable) {
        queryThreadPool.submit(() -> {
            ActiveTraces.setLocal(traces);
            try {
                runnable.run();
            } finally {
                countDownLatch.countDown();
            }
        });
    }

    private MinutePath getSpokeTtlPath(DateTime now) {
        return new MinutePath(now.minusMinutes(HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE) - 2));
    }

    private void incrementMetric(VerifierMetrics verifierMetric) {
        metricsService.increment(verifierMetric.getName());
    }

    private class S3ScheduledVerifierService extends AbstractScheduledService implements Lockable {

        @Override
        protected void runOneIteration() throws Exception {
            DistributedAsynchronousLockRunner distributedLockRunner = new DistributedAsynchronousLockRunner(LEADER_PATH, distributedLeadershipLockManager);
            distributedLockRunner.runWithLock(this, 1, TimeUnit.SECONDS);
        }

        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, offsetMinutes, TimeUnit.MINUTES);
        }

        @Override
        public void takeLeadership(Leadership leadership) throws Exception {
            logger.info("taking leadership");
            while (leadership.hasLeadership()) {
                long start = System.currentTimeMillis();
                verifySingleChannels();
                long sleep = TimeUnit.MINUTES.toMillis(offsetMinutes) - (System.currentTimeMillis() - start);
                logger.debug("sleeping for {} ms", sleep);
                Sleeper.sleep(Math.max(0, sleep));
                logger.debug("waking up after sleep");
            }
            logger.info("lost leadership");
        }
    }
}
