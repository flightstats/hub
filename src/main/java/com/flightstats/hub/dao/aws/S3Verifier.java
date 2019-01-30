package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.dao.aws.s3verifier.MissingContentFinder;
import com.flightstats.hub.dao.aws.s3verifier.VerifierConfig;
import com.flightstats.hub.dao.aws.s3verifier.VerifierRange;
import com.flightstats.hub.dao.aws.s3verifier.VerifierRangeLookup;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
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
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class S3Verifier {
    public static final String LAST_SINGLE_VERIFIED = "/S3VerifierSingleLastVerified/";
    private final static Logger logger = LoggerFactory.getLogger(S3Verifier.class);
    private static final String LEADER_PATH = "/S3VerifierSingleService";
    public static final String MISSING_ITEM_METRIC_NAME = "s3.verifier.missing";
    private static final String VERIFIER_FAILED_METRIC_NAME = "s3.verifier.failed";

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
    protected void verifyChannel(VerifierRange range) {
        String channelName = range.getChannelConfig().getDisplayName();
        SortedSet<ContentKey> keysToAdd = missingContentFinder.getMissing(range.getStartPath(), range.getEndPath(), channelName);
        logger.debug("verifyChannel.starting {}", range);
        for (ContentKey key : keysToAdd) {
            logger.trace("found missing {} {}", channelName, key);
            metricsService.increment(MISSING_ITEM_METRIC_NAME);
            boolean success = s3WriteQueue.add(new ChannelContentKey(channelName, key));
            if (!success) {
                logger.error("unable to queue missing item {} {}", channelName, key);
                metricsService.increment(VERIFIER_FAILED_METRIC_NAME);
                return;
            }
        }
        logger.debug("verifyChannel.completed {}", range);
        lastContentPath.updateIncrease(range.getEndPath(), range.getChannelConfig().getDisplayName(), LAST_SINGLE_VERIFIED);
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
