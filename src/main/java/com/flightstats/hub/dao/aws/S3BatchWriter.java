package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.channel.ZipBulkBuilder;
import com.flightstats.hub.cluster.*;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.*;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.base.Optional;
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
import java.util.concurrent.TimeUnit;

@Singleton
public class S3BatchWriter {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchWriter.class);

    //todo - gfm - this delete can go away once deployed everywhere
    private static final String LAST_BATCH_VERIFIED_OLD = "/S3VerifierBatchLastVerified/";
    public static final String S3_BATCH_WRITER = "/S3BatchWriter/";
    private static final String LEADER_PATH = "/S3BatchWriterLeader";

    private final int offsetMinutes = HubProperties.getProperty("s3BatchWriter.offsetMinutes", 1);
    private final int lagMinutes = HubProperties.getProperty("s3BatchWriter.lagMinutes", 10);

    private final RegulatedConfig regulatedConfig = RegulatedConfig.builder()
            .startThreads(HubProperties.getProperty("s3BatchWriter.channelThreads", 3))
            .maxThreads(30)
            .name("S3BatchWriter")
            .build();

    private RegulatedExecutor regulatedExecutor = new RegulatedExecutor(regulatedConfig);

    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private ChannelService channelService;
    @Inject
    private WebhookService webhookService;
    @Inject
    private MetricsService metricsService;

    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;

    @Inject
    private Client followClient;
    @Inject
    private ZooKeeperState zooKeeperState;
    @Inject
    private CuratorFramework curator;

    public S3BatchWriter() {
        if (HubProperties.getProperty("s3Verifier.run", true)) {
            HubServices.register(new S3BatchWriterService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
        }
    }

    private void writeBatchChannels() {
        try {
            logger.info("Writing Batch S3 data");
            metricsService.gauge("s3batch.threads", regulatedExecutor.getCurrentThreads());
            DateTime start = TimeUtil.now();
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isBatch() || channel.isBoth()) {
                    regulatedExecutor.runAsync(() -> {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName(name + "|" + channel.getDisplayName());
                        String url = HubProperties.getAppUrl() + "internal/s3BatchWriter/" + channel.getDisplayName();
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
            regulatedExecutor.join();
            metricsService.time("s3batch.total", start.getMillis());
            logger.info("Completed Writing Batch S3 data");
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    void writeChannel(String channelName) {
        ChannelConfig channel = channelService.getChannelConfig(channelName, false);
        if (channel == null) {
            return;
        }

        DateTime start = TimeUtil.now();
        MinutePath lagTime = new MinutePath(start.minusMinutes(lagMinutes));
        logger.info("{} starting at {}", channelName, lagTime);
        String webhookName = S3Batch.getGroupName(channelName);
        Optional<Webhook> webhook = webhookService.get(webhookName);

        ContentPath lastWritten = lastContentPath.getOrNull(channelName, S3_BATCH_WRITER);
        if (lastWritten == null) {
            logger.info("no last written {}", channelName);
            if (webhook.isPresent()) {
                lastWritten = webhookService.getStatus(webhook.get()).getLastCompleted();
            } else {
                lastWritten = lagTime;
            }
            logger.info("{} initialize last written to {}", lastWritten);
            lastContentPath.initialize(channelName, lastWritten, S3_BATCH_WRITER);
        } else {
            logger.info("{} existing last written to {}", channelName, lastWritten);
        }

        lastContentPath.delete(channelName, LAST_BATCH_VERIFIED_OLD);
        if (webhook.isPresent()) {
            webhookService.delete(webhookName);
            logger.info("deleted webhook");
        }

        while (lastWritten.getTime().isBefore(lagTime.getTime())) {
            lastWritten = new MinutePath(lastWritten.getTime().plusMinutes(1));
            logger.debug("{} processing {}", channelName, lastWritten);
            TimeQuery timeQuery = TimeQuery.builder().channelName(channelName)
                    .startTime(lastWritten.getTime())
                    .stable(true)
                    .unit(TimeUtil.Unit.MINUTES)
                    .location(Location.CACHE_WRITE)
                    .build();
            SortedSet<ContentKey> keys = channelService.queryByTime(timeQuery);
            if (keys.size() > 0) {
                logger.debug("updating {} lastWritten {} first {} last {}", channelName, lastWritten, keys.first(), keys.last());
                byte[] bytes = ZipBulkBuilder.build(keys, channelName, channelService, false, true);
                s3BatchContentDao.writeBatch(channelName, lastWritten, keys, bytes);
            }
            lastContentPath.updateIncrease(lastWritten, channelName, S3_BATCH_WRITER);
            metricsService.time("s3batch.delta", lastWritten.getTime().getMillis(), "channelName:" + channelName);
            logger.debug("{} updated {} with {} keys", channelName, lastWritten, keys.size());
        }
        metricsService.time("s3batch.processing", start.getMillis(), "channelName:" + channelName);
    }

    private class S3BatchWriterService extends AbstractScheduledService implements Lockable {

        @Override
        protected void runOneIteration() throws Exception {
            CuratorLock curatorLock = new CuratorLock(curator, zooKeeperState, LEADER_PATH);
            curatorLock.runWithLock(this, 1, TimeUnit.SECONDS);
        }

        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, offsetMinutes, TimeUnit.MINUTES);
        }

        @Override
        public void takeLeadership(Leadership leadership) throws Exception {
            logger.info("taking leadership");
            while (leadership.hasLeadership()) {
                long start = System.currentTimeMillis();
                writeBatchChannels();
                long sleep = TimeUnit.MINUTES.toMillis(offsetMinutes) - (System.currentTimeMillis() - start);
                logger.debug("sleeping for {} ms", sleep);
                Sleeper.sleep(Math.max(0, sleep));
                logger.debug("waking up after sleep");
            }
            logger.info("lost leadership");
        }
    }
}
