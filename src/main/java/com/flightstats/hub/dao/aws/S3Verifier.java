package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
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

import static com.flightstats.hub.dao.LocalChannelService.HISTORICAL_FIRST_UPDATED;

@Singleton
public class S3Verifier {

    static final String LAST_SINGLE_VERIFIED = "/S3VerifierSingleLastVerified/";
    private final static Logger logger = LoggerFactory.getLogger(S3Verifier.class);

    private final int offsetMinutes = HubProperties.getProperty("s3Verifier.offsetMinutes", 15);
    private final double keepLeadershipRate = HubProperties.getProperty("s3Verifier.keepLeadershipRate", 0.75);
    private final ExecutorService queryThreadPool = Executors.newFixedThreadPool(30, new ThreadFactoryBuilder().setNameFormat("S3QueryThread-%d").build());
    private final ExecutorService channelThreadPool = Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("S3ChannelThread-%d").build());
    @Inject
    private
    LastContentPath lastContentPath;
    @Inject
    private ChannelService channelService;
    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;
    @Inject
    private S3WriteQueue s3WriteQueue;

    public S3Verifier() {
        if (HubProperties.getProperty("s3Verifier.run", true)) {
            HubServices.register(new S3VerifierService("/S3VerifierSingleService", offsetMinutes, this::runSingle),
                    HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        }
    }

    private SortedSet<ContentKey> getMissing(MinutePath startPath, MinutePath endPath, String channelName, ContentDao s3ContentDao,
                                             SortedSet<ContentKey> foundCacheKeys) {
        QueryResult queryResult = new QueryResult(1);
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
            CountDownLatch latch = new CountDownLatch(2);
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> {
                SortedSet<ContentKey> spokeKeys = spokeContentDao.queryByTime(timeQuery);
                foundCacheKeys.addAll(spokeKeys);
                queryResult.addKeys(spokeKeys);
            });
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> longTermKeys.addAll(s3ContentDao.queryByTime(timeQuery)));
            latch.await(1, TimeUnit.MINUTES);
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

    private void singleS3Verification(VerifierRange range) {
        submit(range, "single", () -> {
            String channelName = range.channel.getName();
            SortedSet<ContentKey> keysToAdd = getMissing(range.startPath, range.endPath, channelName, s3SingleContentDao, new TreeSet<>());
            logger.debug("singleS3Verification.starting {}", range);
            for (ContentKey key : keysToAdd) {
                logger.trace("found missing {} {}", channelName, key);
                s3WriteQueue.add(new ChannelContentKey(channelName, key));
            }
            logger.debug("singleS3Verification.completed {}", range);
            lastContentPath.updateIncrease(range.endPath, range.channel.getName(), LAST_SINGLE_VERIFIED);
        });
    }

    private void submit(VerifierRange range, String typeName, Runnable runnable) {
        channelThreadPool.submit(() -> {
            try {
                MinutePath currentPath = range.startPath;
                String channelName = range.channel.getName();
                Thread.currentThread().setName("s3-" + typeName + "-" + channelName + "-" + currentPath.toUrl());
                ActiveTraces.start("S3Verifier", typeName, range);
                logger.debug("S3Verification {} {}", typeName, range);
                runnable.run();
            } catch (Exception e) {
                logger.error("S3Verifier Error" + typeName + ": " + range, e);
            } finally {
                ActiveTraces.end();
            }
        });
    }

    private void runSingle() {
        try {
            DateTime now = TimeUtil.now();
            logger.info("Verifying Single S3 data at: {}", now);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isSingle() || channel.isBoth()) {
                    VerifierRange range;
                    if (channel.isHistorical()) {
                        range = getHistoricalVerifierRange(now, channel);
                    } else {
                        range = getSingleVerifierRange(now, channel);
                    }
                    if (range != null) {
                        singleS3Verification(range);
                    }
                }
            }
            logger.info("Completed Verifying Single S3 data at: {}", now);
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    VerifierRange getHistoricalVerifierRange(DateTime now, ChannelConfig channel) {
        ContentPath lastUpdated = channelService.getLastUpdated(channel.getName(), new MinutePath(now));
        logger.debug("last updated {} {}", channel.getName(), lastUpdated);
        if (lastUpdated.equals(ContentKey.NONE)) {
            logger.debug("lastUpdated is none - ignore {}", channel.getName());
            return null;
        }
        VerifierRange range = new VerifierRange(channel);
        range.endPath = new MinutePath(lastUpdated.getTime());
        ContentPath firstUpdated = lastContentPath.get(channel.getName(), range.endPath, HISTORICAL_FIRST_UPDATED);
        if (lastUpdated.equals(firstUpdated)) {
            logger.debug("equals {} {}", lastUpdated, firstUpdated);
            range.startPath = range.endPath;
        } else {
            logger.debug("not equal {} {}", lastUpdated, firstUpdated);
            ContentPath lastVerified = lastContentPath.getOrNull(channel.getName(), LAST_SINGLE_VERIFIED);
            if (lastVerified == null) {
                range.startPath = new MinutePath(firstUpdated.getTime());
            } else {
                range.startPath = (MinutePath) lastVerified;
            }
        }
        return range;
    }

    VerifierRange getSingleVerifierRange(DateTime now, ChannelConfig channel) {
        VerifierRange range = new VerifierRange(channel);
        MinutePath spokeTtlTime = getSpokeTtlPath(now);
        now = channelService.getLastUpdated(channel.getName(), new MinutePath(now)).getTime();
        DateTime start = now.minusMinutes(1);
        range.endPath = new MinutePath(start);
        MinutePath defaultStart = new MinutePath(start.minusMinutes(offsetMinutes));
        range.startPath = (MinutePath) lastContentPath.get(channel.getName(), defaultStart, LAST_SINGLE_VERIFIED);
        if (channel.isLive() && range.startPath.compareTo(spokeTtlTime) < 0) {
            range.startPath = spokeTtlTime;
        }
        return range;
    }

    private MinutePath getSpokeTtlPath(DateTime now) {
        return new MinutePath(now.minusMinutes(HubProperties.getSpokeTtl() - 2));
    }

    @ToString
    class VerifierRange {

        MinutePath startPath;
        MinutePath endPath;
        ChannelConfig channel;

        VerifierRange(ChannelConfig channel) {
            this.channel = channel;
        }
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
            logger.info("taking leadership");
            while (hasLeadership.get()) {
                long start = System.currentTimeMillis();
                runnable.run();
                long sleep = TimeUnit.MINUTES.toMillis(minutes) - (System.currentTimeMillis() - start);
                logger.debug("sleeping for {} ms", sleep);
                Sleeper.sleep(Math.max(0, sleep));
                logger.debug("waking up after sleep");
            }
            logger.info("lost leadership");
        }
    }
}
