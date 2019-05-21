package com.flightstats.hub.config.binding;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.AppUrlCheck;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.cluster.SpokeDecommissionManager;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.dao.CachedDao;
import com.flightstats.hub.dao.CachedLowerCaseDao;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.DocumentationDao;
import com.flightstats.hub.dao.ReadOnlyContentDao;
import com.flightstats.hub.dao.ReadOnlyDao;
import com.flightstats.hub.dao.ReadOnlyDocumentationDao;
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
import com.flightstats.hub.dao.aws.S3WriteQueue;
import com.flightstats.hub.dao.aws.S3WriteQueueLifecycle;
import com.flightstats.hub.dao.aws.writeQueue.NoOpWriteQueue;
import com.flightstats.hub.dao.aws.writeQueue.WriteQueue;
import com.flightstats.hub.metrics.PeriodicMetricEmitter;
import com.flightstats.hub.metrics.PeriodicMetricEmitterLifecycle;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.spoke.SpokeContentDao;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.spoke.SpokeTtlEnforcer;
import com.flightstats.hub.webhook.Webhook;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClusterHubBindings extends AbstractModule {

    @Override
    protected void configure() {
        log.info("starting server {} ", HubHost.getLocalName());

        bind(AwsConnectorFactory.class).asEagerSingleton();
        bind(HubS3Client.class).asEagerSingleton();

        bind(LargeContentUtils.class).asEagerSingleton();
        bind(ContentService.class)
                .to(ClusterContentService.class).asEagerSingleton();
        bind(DynamoUtils.class).asEagerSingleton();
        bind(AppUrlCheck.class).asEagerSingleton();

        bind(SpokeDecommissionManager.class).asEagerSingleton();

        bind(PeriodicMetricEmitter.class).asEagerSingleton();
        bind(PeriodicMetricEmitterLifecycle.class).asEagerSingleton();

        bind(S3Config.class).asEagerSingleton();
        bind(S3WriteQueueLifecycle.class).asEagerSingleton();
        bind(S3BatchManager.class).asEagerSingleton();
        bind(S3Verifier.class).asEagerSingleton();
        bind(S3AccessMonitor.class).asEagerSingleton();
        bind(HubS3Client.class).asEagerSingleton();
        bind(S3DocumentationDao.class).asEagerSingleton();
    }

    @Named("READ")
    @Provides
    public static SpokeTtlEnforcer spokeTtlEnforcerRead(ChannelService channelService,
                                                        SpokeContentDao spokeContentDao,
                                                        StatsdReporter statsdReporter,
                                                        SpokeProperties spokeProperties) {
        return new SpokeTtlEnforcer(SpokeStore.READ, channelService, spokeContentDao, statsdReporter, spokeProperties);
    }

    @Named("WRITE")
    @Provides
    public static SpokeTtlEnforcer spokeTtlEnforcerWrite(ChannelService channelService,
                                                         SpokeContentDao spokeContentDao,
                                                         StatsdReporter statsdReporter,
                                                         SpokeProperties spokeProperties) {
        return new SpokeTtlEnforcer(SpokeStore.WRITE, channelService, spokeContentDao, statsdReporter, spokeProperties);
    }

    @Singleton
    @Provides
    public WriteQueue buildWriteQueue(AppProperties props, S3WriteQueue s3Queue) {
        return props.isReadOnly() ? new NoOpWriteQueue() : s3Queue;
    }

    @Singleton
    @Provides
    public DocumentationDao buildDocumentationDao(S3DocumentationDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyDocumentationDao(base) : base;
    }

    @Singleton
    @Provides
    @Named(ContentDao.SINGLE_LONG_TERM)
    public ContentDao buildSingleLongTermContentDao(S3SingleContentDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Singleton
    @Provides
    @Named(ContentDao.LARGE_PAYLOAD)
    public ContentDao buildLargePayloadContentDao(S3LargeContentDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Singleton
    @Provides
    @Named(ContentDao.BATCH_LONG_TERM)
    public ContentDao buildBatchLongTermContentDao(S3BatchContentDao base, AppProperties appProperties) {
        return appProperties.isReadOnly() ? new ReadOnlyContentDao(base) : base;
    }

    @Singleton
    @Provides
    @Named("ChannelConfig")
    public static Dao<ChannelConfig> buildChannelConfigDao(WatchManager watchManager, DynamoChannelConfigDao dao, AppProperties appProperties) {
        Dao<ChannelConfig> cfgDao = new CachedLowerCaseDao<>(dao, watchManager, "/channels/cache");
        return appProperties.isReadOnly() ? new ReadOnlyDao<>(cfgDao) : cfgDao;
    }

    @Singleton
    @Provides
    @Named("Webhook")
    public static Dao<Webhook> buildWebhookDao(WatchManager watchManager, DynamoWebhookDao dao, AppProperties appProperties) {
        Dao<Webhook> whDao = new CachedDao<>(dao, watchManager, "/webhooks/cache");
        return appProperties.isReadOnly() ? new ReadOnlyDao<>(whDao) : whDao;
    }

    @Provides
    @Singleton
    public AmazonDynamoDB buildDynamoClient(AwsConnectorFactory factory) {
        return factory.getDynamoClient();
    }

    @Provides
    @Singleton
    public AmazonS3 buildS3Client(AwsConnectorFactory factory) {
        return factory.getS3Client();
    }
}

