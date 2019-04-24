package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.DocumentationDao;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.ClusterContentService;
import com.flightstats.hub.dao.aws.DynamoChannelConfigDao;
import com.flightstats.hub.dao.aws.DynamoUtils;
import com.flightstats.hub.dao.aws.DynamoWebhookDao;
import com.flightstats.hub.dao.aws.HubS3Client;
import com.flightstats.hub.dao.aws.S3AccessMonitor;
import com.flightstats.hub.dao.aws.S3BatchContentDao;
import com.flightstats.hub.dao.aws.S3BatchManager;
import com.flightstats.hub.dao.aws.S3Config;
import com.flightstats.hub.dao.aws.S3DocumentationDao;
import com.flightstats.hub.dao.aws.S3LargeContentDao;
import com.flightstats.hub.dao.aws.S3SingleContentDao;
import com.flightstats.hub.dao.aws.S3Verifier;
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.metrics.PeriodicS3MetricEmitter;
import com.flightstats.hub.metrics.PeriodicSpokeMetricEmitter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

class ClusterHubBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(ClusterHubBindings.class);

    @Override
    protected void configure() {
        logger.info("starting server {} ", HubHost.getLocalName());

        // General AWS
        bind(AwsConnectorFactory.class).asEagerSingleton();  // very AWS-specific...S3/Dynamo connectors, credentials, etc.

        // S3-specific
        bind(ContentService.class)
                .to(ClusterContentService.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.SINGLE_LONG_TERM))
                .to(S3SingleContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.BATCH_LONG_TERM))
                .to(S3BatchContentDao.class).asEagerSingleton();
        bind(ContentDao.class)
                .annotatedWith(Names.named(ContentDao.LARGE_PAYLOAD))
                .to(S3LargeContentDao.class).asEagerSingleton();
        bind(S3BatchManager.class).asEagerSingleton();
        bind(DocumentationDao.class).to(S3DocumentationDao.class).asEagerSingleton();
        bind(HubS3Client.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();  // sets up a lifecycle service to cull S3 of old records if channel has max items and set TTLs appropriately
        bind(S3Verifier.class).asEagerSingleton();
        bind(PeriodicS3MetricEmitter.class).asEagerSingleton();
        bind(S3AccessMonitor.class).asEagerSingleton();

        // Dynamo-specific
        bind(DynamoUtils.class).asEagerSingleton();
        bind(Dao.class)
                .annotatedWith(Names.named("ChannelConfigDao"))
                .to(DynamoChannelConfigDao.class)
                .asEagerSingleton();
        bind(Dao.class)
                .annotatedWith(Names.named("WebhookDao"))
                .to(DynamoWebhookDao.class)
                .asEagerSingleton();

        // Used only by AWS code.
        // This is S3-specific because spoke stores the full item, instead of an index.
        bind(LargeContentUtils.class).asEagerSingleton();  // Not AWS - Used only by ClusterContentService to store indexes instead of documents on S3.
    }

    @Inject
    @Provides
    @Singleton
    public AmazonDynamoDB buildDynamoClient(AwsConnectorFactory factory) throws IOException {
        return factory.getDynamoClient();
    }

    @Inject
    @Provides
    @Singleton
    public AmazonS3 buildS3Client(AwsConnectorFactory factory) throws IOException {
        return factory.getS3Client();
    }

    @Provides
    @Inject
    @Singleton
    @Named("ChannelConfigDao")
    public Dao<ChannelConfig> buildChannelConfigDao(DynamoChannelConfigDao dao) {
        return dao;
    }

    @Provides
    @Inject
    @Singleton
    @Named("WebhookDao")
    public Dao<Webhook> buildWebhookDao(DynamoWebhookDao dao) {
        return dao;
    }

    @Provides
    @Inject
    @Singleton
    @Named("PeriodicMetricEmitters")
    public Collection<PeriodicMetricEmitter> buildPeriodicMetricEmitters(PeriodicSpokeMetricEmitter spokeEmitter, PeriodicS3MetricEmitter s3Emitter) {
        return Arrays.asList(spokeEmitter, s3Emitter);
    }
}