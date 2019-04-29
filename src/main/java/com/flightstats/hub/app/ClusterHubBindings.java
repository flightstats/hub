package com.flightstats.hub.app;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.cluster.SpokeDecommissionManager;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.DocumentationDao;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.ReadOnlyContentDao;
import com.flightstats.hub.dao.ReadOnlyDao;
import com.flightstats.hub.dao.ReadOnlyDocumentationDao;
import com.flightstats.hub.dao.aws.DynamoWebhookDao;
import com.flightstats.hub.dao.aws.DynamoChannelConfigDao;
import com.flightstats.hub.dao.CachedLowerCaseDao   ;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.S3Config;
import com.flightstats.hub.dao.aws.ClusterContentService;
import com.flightstats.hub.dao.aws.S3SingleContentDao;
import com.flightstats.hub.dao.aws.S3BatchContentDao;
import com.flightstats.hub.dao.aws.S3LargeContentDao;
import com.flightstats.hub.dao.aws.S3AccessMonitor;
import com.flightstats.hub.dao.aws.HubS3Client;
import com.flightstats.hub.dao.aws.S3BatchManager;
import com.flightstats.hub.dao.aws.DynamoUtils;
import com.flightstats.hub.dao.aws.S3Verifier;
import com.flightstats.hub.dao.aws.S3DocumentationDao;
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.spoke.RemoteSpokeStore;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
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

class ClusterHubBindings extends AbstractModule {
    private final static Logger logger = LoggerFactory.getLogger(ClusterHubBindings.class);

    @Override
    protected void configure() {
        logger.info("starting server {} ", HubHost.getLocalName());

        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(S3Config.class).asEagerSingleton();
        bind(LargeContentUtils.class).asEagerSingleton();
        bind(ContentService.class)
                .to(ClusterContentService.class).asEagerSingleton();
        bind(RemoteSpokeStore.class).asEagerSingleton();
        bind(DynamoUtils.class).asEagerSingleton();
        bind(S3BatchManager.class).asEagerSingleton();
        bind(S3Verifier.class).asEagerSingleton();
        bind(AppUrlCheck.class).asEagerSingleton();

        bind(SpokeTtlEnforcer.class)
                .annotatedWith(Names.named(SpokeStore.WRITE.name()))
                .toInstance(new SpokeTtlEnforcer(SpokeStore.WRITE));

        bind(SpokeTtlEnforcer.class)
                .annotatedWith(Names.named(SpokeStore.READ.name()))
                .toInstance(new SpokeTtlEnforcer(SpokeStore.READ));

        bind(SpokeDecommissionManager.class).asEagerSingleton();
        bind(HubS3Client.class).asEagerSingleton();
        bind(S3AccessMonitor.class).asEagerSingleton();
        bind(PeriodicMetricEmitter.class).asEagerSingleton();
        bind(PeriodicMetricEmitterLifecycle.class).asEagerSingleton();
    }

    @Inject
    @Singleton
    @Provides
    public DocumentationDao buildDocumentationDao(S3DocumentationDao base) {
        return HubProperties.isReadOnly() ? new ReadOnlyDocumentationDao(base) : base;
    }

    @Inject
    @Singleton
    @Provides
    @Named(ContentDao.SINGLE_LONG_TERM)
    public ContentDao buildSingleLongTermContentDao(S3SingleContentDao base) {
        return HubProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Inject
    @Singleton
    @Provides
    @Named(ContentDao.LARGE_PAYLOAD)
    public ContentDao buildLargePayloadContentDao(S3LargeContentDao base) {
        return HubProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Inject
    @Singleton
    @Provides
    @Named(ContentDao.BATCH_LONG_TERM)
    public ContentDao buildBatchLongTermContentDao(S3BatchContentDao base) {
        return HubProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Inject
    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, DynamoChannelConfigDao dao) {
        Dao<ChannelConfig> cfgDao = new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
        return HubProperties.isReadOnly() ? new ReadOnlyDao<>(cfgDao) : cfgDao;
    }

    @Inject
    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, DynamoWebhookDao dao) {
        Dao<Webhook> whDao = new CachedDao<>(dao, watchManager, "/webhooks/cache");
        return HubProperties.isReadOnly() ? new ReadOnlyDao<>(whDao) : whDao;
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
}
