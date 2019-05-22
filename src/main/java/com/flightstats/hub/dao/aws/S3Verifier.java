package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.DistributedAsyncLockRunner;
import com.flightstats.hub.cluster.DistributedLeaderLockManager;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.s3Verifier.MissingContentFinder;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierConfig;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierMetrics;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierRange;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierRangeLookup;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.constant.NamedBinding.S3_VERIFIER_CHANNEL_THREAD_POOL;
import static com.flightstats.hub.constant.ZookeeperNodes.LAST_SINGLE_VERIFIED;

@Slf4j
@Singleton
public class S3Verifier {
    private static final String LEADER_PATH = "/S3VerifierSingleService";

    private final VerifierConfig verifierConfig;

    private final ExecutorService channelThreadPool;
    private final ClusterCacheDao clusterCacheDao;
    private final S3WriteQueue s3WriteQueue;
    private final Client httpClient;
    private final MissingContentFinder missingContentFinder;
    private final ContentRetriever contentRetriever;
    private final VerifierRangeLookup verifierRangeLookup;
    private final Dao<ChannelConfig> channelConfigDao;
    private final DistributedLeaderLockManager distributedLeaderLockManager;
    private final StatsdReporter statsdReporter;

    @Inject
    public S3Verifier(ClusterCacheDao clusterCacheDao,
                      S3WriteQueue s3WriteQueue,
                      Client httpClient,
                      MissingContentFinder missingContentFinder,
                      ContentRetriever contentRetriever,
                      VerifierRangeLookup verifierRangeLookup,
                      VerifierConfig verifierConfig,
                      @Named(S3_VERIFIER_CHANNEL_THREAD_POOL) ExecutorService channelThreadPool,
                      @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                      DistributedLeaderLockManager distributedLeaderLockManager,
                      StatsdReporter statsdReporter) {
        this.clusterCacheDao = clusterCacheDao;
        this.channelConfigDao = channelConfigDao;
        this.s3WriteQueue = s3WriteQueue;
        this.httpClient = httpClient;
        this.verifierConfig = verifierConfig;
        this.channelThreadPool = channelThreadPool;
        this.missingContentFinder = missingContentFinder;
        this.contentRetriever = contentRetriever;
        this.verifierRangeLookup = verifierRangeLookup;
        this.distributedLeaderLockManager = distributedLeaderLockManager;
        this.statsdReporter = statsdReporter;

        if (verifierConfig.isEnabled()) {
            HubServices.register(new S3ScheduledVerifierService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        }
    }

    private void verifySingleChannels() {
        try {
            log.info("Verifying Single S3 data");
            Iterable<ChannelConfig> channels = channelConfigDao.getAll(false);
            for (ChannelConfig channel : channels) {
                if (channel.isSingle() || channel.isBoth()) {
                    channelThreadPool.submit(() -> {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName(name + "|" + channel.getDisplayName());
                        String url = verifierConfig.getChannelVerifierEndpoint(channel.getDisplayName());
                        log.debug("calling {}", url);
                        ClientResponse post = null;
                        try {
                            post = httpClient.resource(url).post(ClientResponse.class);
                            log.debug("response from post {}", post);
                        } finally {
                            HubUtils.close(post);
                            Thread.currentThread().setName(name);
                        }
                    });
                }
            }
            log.info("Completed Verifying Single S3 data");
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }

    void verifyChannel(String channelName) {
        DateTime now = TimeUtil.now();
        contentRetriever.getChannelConfig(channelName, false)
                .map(channel -> verifierRangeLookup.getSingleVerifierRange(now, channel))
                .ifPresent(this::verifyChannel);
    }


    @VisibleForTesting
    void verifyChannel(VerifierRange range) {
        String channelName = range.getChannelConfig().getDisplayName();
        SortedSet<ContentKey> keysToAdd = missingContentFinder.getMissing(range.getStartPath(), range.getEndPath(), channelName);
        log.debug("verifyChannel.starting {}", range);
        MinutePath lastCompleted = range.getEndPath();
        for (ContentKey key : keysToAdd) {
            log.trace("found missing {} {}", channelName, key);
            incrementMetric(VerifierMetrics.MISSING_ITEM);
            boolean success = s3WriteQueue.add(new ChannelContentKey(channelName, key));
            if (!success) {
                log.error("unable to queue missing item {} {}", channelName, key);
                incrementMetric(VerifierMetrics.FAILED);
                lastCompleted = new MinutePath(key.getTime().minusMinutes(1));
                break;
            }
        }

        if (isLastCompletedAfterVerifierStart(lastCompleted, range)) {
            log.debug("verifyChannel.completed {}", range);
            clusterCacheDao.setIfAfter(lastCompleted, range.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
            incrementMetric(VerifierMetrics.PARTIAL_UPDATE);
        } else {
            log.warn("verifyChannel completed, but start time is the same as last completed");
            incrementMetric(VerifierMetrics.EXCESSIVE_CHANNEL_VOLUME);
        }
    }

    private boolean isLastCompletedAfterVerifierStart(MinutePath lastCompleted, VerifierRange range) {
        return lastCompleted.compareTo(range.getStartPath()) > 0;
    }

    private void incrementMetric(VerifierMetrics verifierMetric) {
        statsdReporter.increment(verifierMetric.getName());
    }

    private class S3ScheduledVerifierService extends AbstractScheduledService implements Lockable {
        @Override
        protected void runOneIteration() {
            DistributedAsyncLockRunner distributedLockRunner = new DistributedAsyncLockRunner(LEADER_PATH, distributedLeaderLockManager);
            distributedLockRunner.runWithLock(this, 1, TimeUnit.SECONDS);
        }

        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, verifierConfig.getOffsetMinutes(), TimeUnit.MINUTES);
        }

        @Override
        public void takeLeadership(Leadership leadership) {
            log.info("taking leadership");
            while (leadership.hasLeadership()) {
                long start = System.currentTimeMillis();
                verifySingleChannels();
                long sleep = TimeUnit.MINUTES.toMillis(verifierConfig.getOffsetMinutes()) - (System.currentTimeMillis() - start);
                log.debug("sleeping for {} ms", sleep);
                Sleeper.sleep(Math.max(0, sleep));
                log.debug("waking up after sleep");
            }
            log.info("lost leadership");
        }
    }
}
