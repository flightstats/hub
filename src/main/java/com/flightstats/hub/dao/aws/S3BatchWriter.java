package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.*;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Singleton
public class S3BatchWriter {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchWriter.class);

    public static final String S3_BATCH_WRITER = "/S3BatchWriter/";
    private static final String LEADER_PATH = "/S3BatchWriterLeader";

    private final int offsetMinutes = HubProperties.getProperty("s3BatchWriter.offsetMinutes", 1);

    private final RegulatedConfig regulatedConfig = RegulatedConfig.builder()
            .startThreads(HubProperties.getProperty("s3BatchWriter.channelThreads", 3))
            .maxThreads(30)
            .name("S3BatchWriter")
            .build();

    private RegulatedExecutor regulatedExecutor = new RegulatedExecutor(regulatedConfig);

    @Inject
    private ChannelService channelService;

    @Inject
    private MetricsService metricsService;

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
                    regulatedExecutor.runAsync(channel.getName(), () -> {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName(name + "|" + channel.getDisplayName());
                        String endpoint = HubProperties.getProperty("s3BatchWriter.endpoint", "s3BatchWriter");
                        String url = HubProperties.getAppUrl() + "internal/" + endpoint + "/" + channel.getDisplayName();
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
