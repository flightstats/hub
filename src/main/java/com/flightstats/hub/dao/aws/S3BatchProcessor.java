package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.ZipBulkBuilder;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;

import static com.flightstats.hub.dao.aws.S3BatchWriter.S3_BATCH_WRITER;

class S3BatchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(S3BatchProcessor.class);

    //todo - gfm - this delete can go away once deployed everywhere
    private static final String LAST_BATCH_VERIFIED_OLD = "/S3VerifierBatchLastVerified/";

    private final int lagMinutes = HubProperties.getProperty("s3BatchWriter.lagMinutes", 10);

    @Inject
    private LastContentPath lastContentPath;

    @Inject
    private WebhookService webhookService;

    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;

    @Inject
    private ChannelService channelService;

    @Inject
    private MetricsService metricsService;

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

}
