package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.aws.s3Verifier.MissingContentFinder;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierMetrics;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierConfig;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierRange;
import com.flightstats.hub.dao.aws.s3Verifier.VerifierRangeLookup;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class S3Verifier {
    public static final String LAST_SINGLE_VERIFIED = "/S3VerifierSingleLastVerified/";
    private final static Logger logger = LoggerFactory.getLogger(S3Verifier.class);
    private static final String LEADER_PATH = "/S3VerifierSingleService";

    private final VerifierConfig verifierConfig;

    private final ExecutorService channelThreadPool;
    private final LastContentPath lastContentPath;
    private final ChannelService channelService;
    private final S3WriteQueue s3WriteQueue;
    private final Client httpClient;
    private final ZooKeeperState zooKeeperState;
    private final CuratorFramework curator;
    private final MetricsService metricsService;
    private final MissingContentFinder missingContentFinder;
    private final VerifierRangeLookup verifierRangeLookup;

    @Inject
    public S3Verifier(LastContentPath lastContentPath,
                      ChannelService channelService,
                      S3WriteQueue s3WriteQueue,
                      Client httpClient,
                      ZooKeeperState zooKeeperState,
                      CuratorFramework curator,
                      MetricsService metricsService,
                      MissingContentFinder missingContentFinder,
                      VerifierRangeLookup verifierRangeLookup,
                      VerifierConfig verifierConfig,
                      @Named("s3VerifierChannelThreadPool") ExecutorService channelThreadPool) {
        this.lastContentPath = lastContentPath;
        this.channelService = channelService;
        this.s3WriteQueue = s3WriteQueue;
        this.httpClient = httpClient;
        this.zooKeeperState = zooKeeperState;
        this.curator = curator;
        this.metricsService = metricsService;
        this.verifierConfig = verifierConfig;
        this.channelThreadPool = channelThreadPool;
        this.missingContentFinder = missingContentFinder;
        this.verifierRangeLookup = verifierRangeLookup;

        if (verifierConfig.isEnabled()) {
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
                        String url = verifierConfig.getEndpointUrlGenerator().apply(channel.getDisplayName());
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
        VerifierRange range = verifierRangeLookup.getSingleVerifierRange(now, channel);
        if (range != null) {
            verifyChannel(range);
        }
    }


    @VisibleForTesting
    void verifyChannel(VerifierRange range) {
        String channelName = range.getChannelConfig().getDisplayName();
        SortedSet<ContentKey> keysToAdd = missingContentFinder.getMissing(range.getStartPath(), range.getEndPath(), channelName);
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

    private void incrementMetric(VerifierMetrics verifierMetric) {
        metricsService.increment(verifierMetric.getName());
    }

    private class S3ScheduledVerifierService extends AbstractScheduledService implements Lockable {
        @Override
        protected void runOneIteration() throws Exception {
            CuratorLock curatorLock = new CuratorLock(curator, zooKeeperState, LEADER_PATH);
            curatorLock.runWithLock(this, 1, TimeUnit.SECONDS);
        }

        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, verifierConfig.getOffsetMinutes(), TimeUnit.MINUTES);
        }

        @Override
        public void takeLeadership(Leadership leadership) throws Exception {
            logger.info("taking leadership");
            while (leadership.hasLeadership()) {
                long start = System.currentTimeMillis();
                verifySingleChannels();
                long sleep = TimeUnit.MINUTES.toMillis(verifierConfig.getOffsetMinutes()) - (System.currentTimeMillis() - start);
                logger.debug("sleeping for {} ms", sleep);
                Sleeper.sleep(Math.max(0, sleep));
                logger.debug("waking up after sleep");
            }
            logger.info("lost leadership");
        }
    }
}
