package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLeader;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.Leader;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.AllArgsConstructor;
import lombok.ToString;
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

    private final int offsetMinutes = HubProperties.getProperty("s3Verifier.offsetMinutes", 15);
    private final int channelThreads = HubProperties.getProperty("s3Verifier.channelThreads", 3);
    private final ExecutorService channelThreadPool = Executors.newFixedThreadPool(channelThreads, new ThreadFactoryBuilder().setNameFormat("S3VerifierChannel-%d").build());
    private final ExecutorService queryThreadPool = Executors.newFixedThreadPool(channelThreads * 2, new ThreadFactoryBuilder().setNameFormat("S3VerifierQuery-%d").build());
    @Inject
    private LastContentPath lastContentPath;
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
    @Inject
    private Client followClient;

    public S3Verifier() {
        if (HubProperties.getProperty("s3Verifier.run", true)) {
            HubServices.register(new S3VerifierService("/S3VerifierSingleService", offsetMinutes, this::verifySingleChannels),
                    HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
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
                        Thread.currentThread().setName(name + "|" + channel.getName());
                        String url = HubProperties.getAppUrl() + "internal/s3Verifier/" + channel.getName();
                        logger.debug("calling {}", url);
                        ClientResponse post = null;
                        try {
                            post = followClient.resource(url).post(ClientResponse.class);
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

    private void verifyChannel(VerifierRange range) {
        String channelName = range.channel.getName();
        SortedSet<ContentKey> keysToAdd = getMissing(range.startPath, range.endPath, channelName, s3SingleContentDao, new TreeSet<>());
        logger.debug("verifyChannel.starting {}", range);
        for (ContentKey key : keysToAdd) {
            logger.trace("found missing {} {}", channelName, key);
            s3WriteQueue.add(new ChannelContentKey(channelName, key));
        }
        logger.debug("verifyChannel.completed {}", range);
        lastContentPath.updateIncrease(range.endPath, range.channel.getName(), LAST_SINGLE_VERIFIED);
    }

    private SortedSet<ContentKey> getMissing(MinutePath startPath, MinutePath endPath, String channelName, ContentDao s3ContentDao,
                                             SortedSet<ContentKey> foundCacheKeys) {
        QueryResult queryResult = new QueryResult(1);
        SortedSet<ContentKey> longTermKeys = new TreeSet<>();
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES);
        long timeout = 1;
        if (endPath != null) {
            Duration duration = new Duration(startPath.getTime(), endPath.getTime());
            timeout += duration.getStandardDays();
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
            latch.await(timeout, TimeUnit.MINUTES);
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
        public void takeLeadership(Leadership leadership) {
            logger.info("taking leadership");
            while (leadership.hasLeadership()) {
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
